[CmdletBinding()]
param(
    [string]$DatabasePath = "app/src/main/assets/databases/quranplus.db",
    [string]$SqlitePath = "sqlite3.exe"
)

$ErrorActionPreference = "Stop"
$apiBase = "https://api.quran.com/api/v4/verses/by_chapter"
$sourceRevisionPrefix = "quran.com-api:wbw-id"

function Invoke-SqliteScalar([string]$query) {
    $value = & $SqlitePath -batch -noheader $DatabasePath $query
    if ($LASTEXITCODE -ne 0) {
        throw "sqlite3 gagal menjalankan query. exit=$LASTEXITCODE"
    }
    return $value.Trim()
}

function Quote-SqlLiteral([string]$value) {
    if ($null -eq $value) {
        return "NULL"
    }
    return "'" + $value.Replace("'", "''") + "'"
}

function Get-WordTranslations {
    $chapters = 1..114
    $responses = $chapters | ForEach-Object -Parallel {
        $chapter = $_
        $uri = "https://api.quran.com/api/v4/verses/by_chapter/${chapter}?words=true&language=id&per_page=300"
        $response = $null
        for ($attempt = 1; $attempt -le 3; $attempt++) {
            try {
                $response = Invoke-RestMethod -Uri $uri -Headers @{ "User-Agent" = "QuranPlus-word-import" } -TimeoutSec 60
                break
            } catch {
                if ($attempt -eq 3) { throw }
                Start-Sleep -Seconds (2 * $attempt)
            }
        }
        [pscustomobject]@{
            Chapter = $chapter
            Verses = $response.verses
            Total = [int]$response.pagination.total_records
        }
    } -ThrottleLimit 8 | Sort-Object Chapter

    $words = foreach ($response in $responses) {
        if ($response.Total -ne $response.Verses.Count) {
            throw "Coverage surah $($response.Chapter) tidak lengkap."
        }
        foreach ($verse in $response.Verses) {
            foreach ($word in $verse.words | Where-Object char_type_name -eq "word") {
                if ($word.translation.language_name -ne "indonesian" -or [string]::IsNullOrWhiteSpace($word.translation.text)) {
                    throw "Terjemahan Indonesia tidak lengkap pada $($response.Chapter):$($verse.verse_number):$($word.position)."
                }
                [pscustomobject]@{
                    Surah = [int]$response.Chapter
                    Ayah = [int]$verse.verse_number
                    Position = [int]$word.position
                    Translation = [string]$word.translation.text
                }
            }
        }
    }

    return @($words | Sort-Object Surah, Ayah, Position)
}

function Get-SourceHash($words) {
    $canonical = ($words | ForEach-Object {
        "$($_.Surah)|$($_.Ayah)|$($_.Position)|$($_.Translation)"
    }) -join "`n"
    $bytes = [Text.Encoding]::UTF8.GetBytes($canonical)
    return ([BitConverter]::ToString(
        [Security.Cryptography.SHA256]::HashData($bytes)
    )).Replace("-", "").ToLowerInvariant()
}

function Update-Database($words, [string]$revision, [string]$sourceHash) {
    $expectedCount = $words.Count
    $actualCount = [int](Invoke-SqliteScalar "SELECT COUNT(*) FROM word_by_word;")
    if ($actualCount -ne $expectedCount) {
        throw "Jumlah baris asset berbeda: database=$actualCount, API=$expectedCount."
    }

    $sql = [Text.StringBuilder]::new()
    [void]$sql.AppendLine("BEGIN IMMEDIATE;")
    foreach ($word in $words) {
        $translation = Quote-SqlLiteral $word.Translation
        [void]$sql.AppendLine(
            "UPDATE word_by_word SET translation_id=$translation, source_revision=$(Quote-SqlLiteral $revision), source_sha256=$(Quote-SqlLiteral $sourceHash) " +
            "WHERE surah_id=$($word.Surah) AND ayah_number=$($word.Ayah) AND word_index=$($word.Position);"
        )
    }
    [void]$sql.AppendLine("COMMIT;")

    $process = [Diagnostics.Process]::new()
    $process.StartInfo.FileName = $SqlitePath
    $process.StartInfo.UseShellExecute = $false
    $process.StartInfo.RedirectStandardInput = $true
    $process.StartInfo.RedirectStandardError = $true
    [void]$process.StartInfo.ArgumentList.Add($DatabasePath)
    [void]$process.Start()
    $process.StandardInput.Write($sql.ToString())
    $process.StandardInput.Close()
    $process.WaitForExit()
    if ($process.ExitCode -ne 0) {
        throw "Import database gagal: $($process.StandardError.ReadToEnd())"
    }
}

$words = Get-WordTranslations
$hash = Get-SourceHash $words
$revision = "${sourceRevisionPrefix}:$hash"
Update-Database $words $revision $hash

$verified = Invoke-SqliteScalar "SELECT COUNT(*) FROM word_by_word WHERE source_revision=$(Quote-SqlLiteral $revision) AND trim(translation_id) <> '';"
if ([int]$verified -ne $words.Count) {
    throw "Verifikasi import gagal: $verified/$($words.Count) baris."
}

Write-Output "Imported $verified Indonesian word translations."
Write-Output "Source revision: $revision"
Write-Output "Source: ${apiBase}?words=true&language=id"
