[CmdletBinding()]
param(
    [Parameter()]
    [string]$ReferenceRoot = (Join-Path $PSScriptRoot '..\docs\HadistReference\reference2'),
    [Parameter()]
    [switch]$AsJson
)

$ErrorActionPreference = 'Stop'
$root = [IO.Path]::GetFullPath($ReferenceRoot)

function Get-Sha256([string]$Path) {
    $digest = [Security.Cryptography.SHA256]::Create()
    try {
        $stream = [IO.File]::OpenRead($Path)
        try {
            return ([BitConverter]::ToString($digest.ComputeHash($stream))).Replace('-', '').ToLowerInvariant()
        } finally {
            $stream.Dispose()
        }
    } finally {
        $digest.Dispose()
    }
}

$books = @(
    @{ name = 'bukhari'; book_id = 1 },
    @{ name = 'muslim'; book_id = 2 },
    @{ name = 'abudawud'; book_id = 4 },
    @{ name = 'tirmidhi'; book_id = 5 }
)
$rows = [Collections.Generic.List[object]]::new()
$globalIds = [Collections.Generic.HashSet[string]]::new()
$compositeIds = [Collections.Generic.HashSet[string]]::new()
$duplicateKeys = [Collections.Generic.HashSet[string]]::new()
$structuralIssues = [Collections.Generic.List[object]]::new()
$incompleteFields = [Collections.Generic.List[string]]::new()
$incompleteSamples = [Collections.Generic.List[object]]::new()
$incompleteRecordKeys = [Collections.Generic.HashSet[string]]::new()
$invalidChapterReferences = [Collections.Generic.List[object]]::new()

foreach ($book in $books) {
    $bookName = $book.name
    $path = Join-Path $root "db\by_book\the_9_books\$bookName.json"
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        throw "Missing hadith source file: $path"
    }

    $document = Get-Content -LiteralPath $path -Raw | ConvertFrom-Json
    $records = @($document.hadiths)
    $chapters = @($document.chapters)
    $chapterIds = [Collections.Generic.HashSet[string]]::new()
    foreach ($chapter in $chapters) {
        $null = $chapterIds.Add([string]$chapter.id)
    }
    $sha256 = Get-Sha256 $path
    $index = 0
    foreach ($record in $records) {
        $required = @('id', 'idInBook', 'chapterId', 'bookId', 'arabic')
        $missing = @($required | Where-Object {
                $null -eq $record.$_ -or [string]::IsNullOrWhiteSpace([string]$record.$_)
            })
        if ($null -eq $record.english) {
            $missing += 'english'
        } else {
            foreach ($field in @('narrator', 'text')) {
                if ([string]::IsNullOrWhiteSpace([string]$record.english.$field)) {
                    $incompleteFields.Add("english.$field")
                    $null = $incompleteRecordKeys.Add("${bookName}:$($record.id)")
                    if ($incompleteSamples.Count -lt 10) {
                        $incompleteSamples.Add([pscustomobject]@{
                                book = $bookName
                                id = $record.id
                                id_in_book = $record.idInBook
                                field = "english.$field"
                            })
                    }
                }
            }
        }
        if (-not $chapterIds.Contains([string]$record.chapterId)) {
            $invalidChapterReferences.Add([pscustomobject]@{
                    book = $bookName
                    index = $index
                    chapter_id = $record.chapterId
                })
        }
        if ($missing.Count -gt 0) {
            $structuralIssues.Add([pscustomobject]@{
                    book = $bookName
                    index = $index
                    fields = @($missing)
                })
        }

        $globalKey = [string]$record.id
        $compositeKey = "$($record.bookId):$($record.idInBook)"
        if (-not $globalIds.Add($globalKey)) {
            $null = $duplicateKeys.Add("global:$globalKey")
        }
        if (-not $compositeIds.Add($compositeKey)) {
            $null = $duplicateKeys.Add("book-id:$compositeKey")
        }
        $index++
    }

    $rows.Add([pscustomobject]@{
            collection = $bookName
            book_id = $book.book_id
            chapter_count = $chapters.Count
            record_count = $records.Count
            metadata_length = $document.metadata.length
            sha256 = $sha256
        })
}

$packagePath = Join-Path $root 'package.json'
$package = Get-Content -LiteralPath $packagePath -Raw | ConvertFrom-Json
$structurallyValid = $structuralIssues.Count -eq 0 -and
    $duplicateKeys.Count -eq 0 -and
    $invalidChapterReferences.Count -eq 0
$result = [pscustomobject]@{
    status = if ($structurallyValid) { 'schema-valid-incomplete-records-license-review-required' } else { 'blocked-schema-validation-failed' }
    source_revision = (& git -C $root rev-parse HEAD).Trim()
    package_version = [string]$package.version
    package_license = [string]$package.license
    total_records = ($rows | Measure-Object -Property record_count -Sum).Sum
    collections = @($rows)
    duplicate_composite_ids = @($duplicateKeys | Sort-Object)
    structural_issue_records = @($structuralIssues)
    incomplete_record_count = $incompleteRecordKeys.Count
    incomplete_field_counts = @(
        $incompleteFields | Group-Object | ForEach-Object {
            [pscustomobject]@{ field = $_.Name; record_count = $_.Count }
        }
    )
    incomplete_samples = @($incompleteSamples)
    invalid_chapter_references = @($invalidChapterReferences)
    policy = @{
        id_in_book_available = ($structuralIssues | Where-Object { $_.fields -contains 'idInBook' }).Count -eq 0
        record_grading_available = $false
        data_license_verified = $false
        bundle_allowed = $false
    }
}

if ($AsJson) {
    $result | ConvertTo-Json -Depth 8
} else {
    $result | Format-List
}

if ($result.status -eq 'blocked-schema-validation-failed') {
    exit 2
}
