package com.quranplus.app.features.hadith.data

import android.content.Context
import android.util.JsonReader
import android.util.JsonToken
import com.quranplus.app.core.database.QuranDatabase
import com.quranplus.app.core.database.entity.HadithChapterEntity
import com.quranplus.app.core.database.entity.HadithCollectionEntity
import com.quranplus.app.core.database.entity.HadithEntity
import com.quranplus.app.core.database.entity.HadithGradeEntity
import com.quranplus.app.core.database.entity.HadithReferenceEntity
import com.quranplus.app.core.database.entity.HadithSchemeStatEntity
import com.quranplus.app.core.database.entity.HadithSourceManifestEntity
import com.quranplus.app.core.database.entity.HadithTranslationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.security.MessageDigest
import java.util.zip.ZipFile

sealed interface CanonicalSyncResult {
    data object AlreadyCurrent : CanonicalSyncResult
    data class Synchronized(val activeRecords: Int, val collectionCount: Int) : CanonicalSyncResult
    data class Failed(val status: String, val reason: String) : CanonicalSyncResult
}

/**
 * Synchronizes the pinned Unlicense + MIT offline Hadith corpus bundle into Room
 * using constant-memory streaming SHA-256 verification and JsonReader batching,
 * preserving user tables and non-conflicting legacy data.
 */
class CanonicalHadithSynchronizer(
    private val context: Context,
    private val database: QuranDatabase
) {
    private val syncMutex = Mutex()

    suspend fun ensureSynchronized(): CanonicalSyncResult = syncMutex.withLock {
        withContext(Dispatchers.IO) {
            val dao = database.hadithDao()
            val manifestJson = runCatching {
                context.assets.open(MANIFEST_ASSET_PATH).bufferedReader().use { it.readText() }
            }.getOrElse { error ->
                return@withContext CanonicalSyncResult.Failed(
                    STATUS_NOT_INSTALLED,
                    "Manifest korpus kanonik tidak ditemukan: ${error.message}"
                )
            }

            val manifest = runCatching { JSONObject(manifestJson) }.getOrElse { error ->
                return@withContext CanonicalSyncResult.Failed(
                    STATUS_CORRUPT,
                    "Manifest korpus kanonik rusak: ${error.message}"
                )
            }

            val sourceId = manifest.getString("source_id")
            val sourceUri = manifest.getString("source_uri")
            val revision = manifest.getString("revision")
            val licenseId = manifest.getString("license_id")
            val expectedArchiveSize = manifest.getLong("archive_size_bytes")
            val expectedArchiveSha = manifest.getString("archive_sha256")
            val expectedActiveCount = manifest.getInt("active_record_count")
            val expectedMissingCount = manifest.optInt("missing_slot_count", 0)

            val recordedManifest = dao.getSourceManifest(sourceId)
            val syncedCount = dao.countCanonicalHadithsByRevisionAndSha(revision, expectedArchiveSha)
            val ahmadStats = dao.getSchemeStatsForCollection("ahmad")
            if (recordedManifest?.status == STATUS_VERIFIED &&
                recordedManifest.revision == revision &&
                recordedManifest.archiveSha256.equals(expectedArchiveSha, ignoreCase = true) &&
                recordedManifest.activeRecordCount == expectedActiveCount &&
                syncedCount == expectedActiveCount &&
                ahmadStats.any { it.schemeId == SCHEME_AR_RISALAH }
            ) {
                return@withContext CanonicalSyncResult.AlreadyCurrent
            }

            val tempArchive = File.createTempFile("sunnah-canonical-", ".zip", context.cacheDir)
            try {
                val (copiedBytes, actualArchiveSha) = copyAssetAndDigest(BUNDLE_ASSET_PATH, tempArchive)
                if (copiedBytes != expectedArchiveSize || !actualArchiveSha.equals(expectedArchiveSha, ignoreCase = true)) {
                    dao.insertSourceManifest(
                        HadithSourceManifestEntity(
                            sourceId = sourceId,
                            sourceUri = sourceUri,
                            revision = revision,
                            archiveSha256 = actualArchiveSha,
                            licenseId = licenseId,
                            activeRecordCount = syncedCount,
                            missingSlotCount = expectedMissingCount,
                            status = STATUS_CORRUPT,
                            updatedAtEpochMs = System.currentTimeMillis()
                        )
                    )
                    return@withContext CanonicalSyncResult.Failed(
                        STATUS_CORRUPT,
                        "Checksum bundle kanonik tidak cocok ($actualArchiveSha != $expectedArchiveSha)"
                    )
                }

                val collectionsObj = manifest.getJSONObject("collections")
                val collectionKeys = collectionsObj.keys().asSequence().toList()

                // Pass 1: Verify every collection JSON entry SHA-256 via streaming digest before DB mutation.
                val collectionShas = HashMap<String, String>(collectionKeys.size)
                ZipFile(tempArchive).use { zip ->
                    for (collectionId in collectionKeys) {
                        val colMeta = collectionsObj.getJSONObject(collectionId)
                        val entryName = colMeta.getString("file")
                        val expectedEntrySha = colMeta.getString("sha256")
                        val entry = zip.getEntry(entryName)
                            ?: return@withContext recordFailure(
                                sourceId, sourceUri, revision, expectedArchiveSha, licenseId,
                                syncedCount, expectedMissingCount, STATUS_CORRUPT,
                                "Entri $entryName hilang dari bundle kanonik"
                            )
                        val actualEntrySha = zip.getInputStream(entry).use { sha256StreamHex(it) }
                        if (!actualEntrySha.equals(expectedEntrySha, ignoreCase = true)) {
                            return@withContext recordFailure(
                                sourceId, sourceUri, revision, expectedArchiveSha, licenseId,
                                syncedCount, expectedMissingCount, STATUS_CORRUPT,
                                "Checksum koleksi $collectionId tidak valid"
                            )
                        }
                        collectionShas[collectionId] = actualEntrySha
                    }

                    // Pass 2: Stream each verified entry atomically inside one Room KMP transaction.
                    var importedTotal = 0
                    var collectionCount = 0
                    dao.runInTransaction {
                        for (collectionId in collectionKeys) {
                            val colMeta = collectionsObj.getJSONObject(collectionId)
                            val entryName = colMeta.getString("file")
                            val entry = zip.getEntry(entryName)!!
                            val count = streamImportCollection(
                                collectionId = collectionId,
                                inputStream = BufferedInputStream(zip.getInputStream(entry), 32_768),
                                sourceRevision = revision,
                                archiveSha256 = expectedArchiveSha,
                                collectionSha256 = collectionShas.getValue(collectionId),
                                colMeta = colMeta
                            )
                            importedTotal += count
                            collectionCount += 1
                        }

                        val finalStatus = if (importedTotal == expectedActiveCount) STATUS_VERIFIED else STATUS_PARTIAL
                        dao.insertSourceManifest(
                            HadithSourceManifestEntity(
                                sourceId = sourceId,
                                sourceUri = sourceUri,
                                revision = revision,
                                archiveSha256 = expectedArchiveSha,
                                licenseId = licenseId,
                                activeRecordCount = importedTotal,
                                missingSlotCount = expectedMissingCount,
                                status = finalStatus,
                                updatedAtEpochMs = System.currentTimeMillis()
                            )
                        )
                    }
                    CanonicalSyncResult.Synchronized(importedTotal, collectionCount)
                }
            } catch (error: Exception) {
                recordFailure(
                    sourceId = sourceId,
                    sourceUri = sourceUri,
                    revision = revision,
                    archiveSha256 = expectedArchiveSha,
                    licenseId = licenseId,
                    activeCount = syncedCount,
                    missingCount = expectedMissingCount,
                    status = STATUS_PARTIAL,
                    reason = error.message ?: "Gagal menyinkronkan korpus hadist kanonik"
                )
            } finally {
                tempArchive.delete()
            }
        }
    }

    private suspend fun streamImportCollection(
        collectionId: String,
        inputStream: InputStream,
        sourceRevision: String,
        archiveSha256: String,
        collectionSha256: String,
        colMeta: JSONObject
    ): Int {
        val dao = database.hadithDao()
        dao.clearCollectionData(collectionId)

        var titleEnglish = colMeta.optString("title_english", collectionId)
        var titleArabic = colMeta.optString("title_arabic", "")
        var defaultScheme = colMeta.optString("default_scheme", SCHEME_SUNNAH)
        var licenseName = colMeta.optString("license_id", "Unlicense")
        var gradedCount = colMeta.optInt("graded_count", 0)
        var expectedMissingSlots = colMeta.optInt("missing_slot_count", 0)

        var licenseStatus = licenseStatusForLicenseName(licenseName)
        var translationPrefix = if (licenseName.equals("MIT", ignoreCase = true)) {
            "irsyadulibad"
        } else {
            "fawazahmed0"
        }

        val chapters = mutableListOf<HadithChapterEntity>()
        val schemeStats = mutableListOf<HadithSchemeStatEntity>()
        val missingRefs = mutableListOf<HadithReferenceEntity>()
        val seenRefKeys = HashSet<String>(4096)

        val hadithBatch = ArrayList<HadithEntity>(STREAM_BATCH_SIZE)
        val refBatch = ArrayList<HadithReferenceEntity>(STREAM_BATCH_SIZE * 2)
        val transBatch = ArrayList<HadithTranslationEntity>(STREAM_BATCH_SIZE * 2)
        val gradeBatch = ArrayList<HadithGradeEntity>(STREAM_BATCH_SIZE)
        var importedCount = 0

        suspend fun flushRecordBatches() {
            if (hadithBatch.isNotEmpty()) {
                dao.insertHadiths(hadithBatch)
                hadithBatch.clear()
            }
            if (refBatch.isNotEmpty()) {
                dao.insertReferences(refBatch)
                refBatch.clear()
            }
            if (transBatch.isNotEmpty()) {
                dao.insertTranslations(transBatch)
                transBatch.clear()
            }
            if (gradeBatch.isNotEmpty()) {
                dao.insertGrades(gradeBatch)
                gradeBatch.clear()
            }
        }

        JsonReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "title_english" -> titleEnglish = reader.nextStringOrEmpty()
                    "title_arabic" -> titleArabic = reader.nextStringOrEmpty()
                    "default_scheme" -> defaultScheme = reader.nextStringOrEmpty().ifBlank { SCHEME_SUNNAH }
                    "license" -> {
                        licenseName = reader.nextStringOrEmpty()
                        licenseStatus = licenseStatusForLicenseName(licenseName)
                        translationPrefix = if (licenseName.equals("MIT", ignoreCase = true)) {
                            "irsyadulibad"
                        } else {
                            "fawazahmed0"
                        }
                    }
                    "chapters" -> parseChaptersArray(reader, collectionId, chapters)
                    "scheme_ranges" -> parseSchemeRangesObject(reader, collectionId, defaultScheme, schemeStats)
                    "missing_slots" -> parseMissingSlotsArray(reader, collectionId, seenRefKeys, missingRefs)
                    "records" -> {
                        reader.beginArray()
                        while (reader.hasNext()) {
                            parseSingleRecord(
                                reader = reader,
                                collectionId = collectionId,
                                titleEnglish = titleEnglish,
                                defaultScheme = defaultScheme,
                                sourceRevision = sourceRevision,
                                archiveSha256 = archiveSha256,
                                licenseStatus = licenseStatus,
                                translationPrefix = translationPrefix,
                                seenRefKeys = seenRefKeys,
                                outHadiths = hadithBatch,
                                outRefs = refBatch,
                                outTranslations = transBatch,
                                outGrades = gradeBatch
                            )
                            importedCount += 1
                            if (hadithBatch.size >= STREAM_BATCH_SIZE) {
                                flushRecordBatches()
                            }
                        }
                        reader.endArray()
                    }
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
        }

        flushRecordBatches()

        if (missingRefs.isNotEmpty()) {
            missingRefs.chunked(STREAM_BATCH_SIZE).forEach { dao.insertReferences(it) }
        }
        if (chapters.isNotEmpty()) {
            chapters.chunked(STREAM_BATCH_SIZE).forEach { dao.insertChapters(it) }
        }
        if (schemeStats.isEmpty()) {
            parseSchemeRangesFromMeta(colMeta, collectionId, defaultScheme, schemeStats)
        }
        if (schemeStats.isNotEmpty()) {
            dao.insertSchemeStats(schemeStats)
        }

        dao.insertCollections(
            listOf(
                HadithCollectionEntity(
                    id = collectionId,
                    titleArabic = titleArabic,
                    titleEnglish = titleEnglish,
                    sourceRevision = sourceRevision,
                    sourceSha256 = collectionSha256,
                    licenseStatus = licenseStatus,
                    gradeStatus = if (gradedCount > 0) "verified_multi_authority" else "not_provided",
                    recordCount = importedCount,
                    chapterCount = chapters.size,
                    isComplete = expectedMissingSlots >= 0,
                    bundleAllowed = true,
                    defaultScheme = defaultScheme,
                    activeRecordCount = importedCount,
                    availabilityStatus = STATUS_AVAILABLE
                )
            )
        )
        return importedCount
    }

    private fun parseChaptersArray(
        reader: JsonReader,
        collectionId: String,
        out: MutableList<HadithChapterEntity>
    ) {
        reader.beginArray()
        var idx = 1
        while (reader.hasNext()) {
            var id = ""
            var number = idx
            var titleArabic = ""
            var titleEnglish = ""
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "id" -> id = reader.nextStringOrEmpty()
                    "number" -> number = reader.nextInt()
                    "title_arabic" -> titleArabic = reader.nextStringOrEmpty()
                    "title_english" -> titleEnglish = reader.nextStringOrEmpty()
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            out.add(
                HadithChapterEntity(
                    collectionId = collectionId,
                    chapterId = id,
                    chapterNumber = number,
                    titleArabic = titleArabic,
                    titleEnglish = titleEnglish
                )
            )
            idx++
        }
        reader.endArray()
    }

    private fun parseSchemeRangesObject(
        reader: JsonReader,
        collectionId: String,
        defaultScheme: String,
        out: MutableList<HadithSchemeStatEntity>
    ) {
        reader.beginObject()
        while (reader.hasNext()) {
            val schemeId = reader.nextName()
            var recordCount = 0
            var minRef = 0
            var maxRef = 0
            var missingCount = 0
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "record_count" -> recordCount = reader.nextInt()
                    "min_reference_number" -> minRef = reader.nextInt()
                    "max_reference_number" -> maxRef = reader.nextInt()
                    "missing_slot_count" -> missingCount = reader.nextInt()
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            out.add(
                HadithSchemeStatEntity(
                    collectionId = collectionId,
                    schemeId = schemeId,
                    schemeLabel = schemeDisplayLabel(schemeId),
                    recordCount = recordCount,
                    minReferenceNumber = minRef,
                    maxReferenceNumber = maxRef,
                    missingSlotCount = missingCount,
                    isDefault = schemeId == defaultScheme
                )
            )
        }
        reader.endObject()
    }

    private fun parseSchemeRangesFromMeta(
        colMeta: JSONObject,
        collectionId: String,
        defaultScheme: String,
        out: MutableList<HadithSchemeStatEntity>
    ) {
        val obj = colMeta.optJSONObject("scheme_ranges") ?: return
        for (schemeId in obj.keys()) {
            val item = obj.getJSONObject(schemeId)
            out.add(
                HadithSchemeStatEntity(
                    collectionId = collectionId,
                    schemeId = schemeId,
                    schemeLabel = schemeDisplayLabel(schemeId),
                    recordCount = item.optInt("record_count", 0),
                    minReferenceNumber = item.optInt("min_reference_number", 0),
                    maxReferenceNumber = item.optInt("max_reference_number", 0),
                    missingSlotCount = item.optInt("missing_slot_count", 0),
                    isDefault = schemeId == defaultScheme
                )
            )
        }
    }

    private fun parseMissingSlotsArray(
        reader: JsonReader,
        collectionId: String,
        seenRefKeys: HashSet<String>,
        out: MutableList<HadithReferenceEntity>
    ) {
        reader.beginArray()
        while (reader.hasNext()) {
            var scheme = SCHEME_SUNNAH
            var number = 0
            var variant = ""
            var reason = REVIEW_MISSING_IN_SOURCE
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "scheme" -> scheme = reader.nextStringOrEmpty()
                    "number" -> number = reader.nextInt()
                    "variant" -> variant = reader.nextStringOrEmpty()
                    "reason" -> reason = reader.nextStringOrEmpty().ifBlank { REVIEW_MISSING_IN_SOURCE }
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            val uniqueKey = "$collectionId|$scheme|$number|$variant"
            if (seenRefKeys.add(uniqueKey)) {
                val missingStable = buildString {
                    append("missing:").append(collectionId).append(':').append(scheme).append(':').append(number)
                    if (variant.isNotBlank()) append('.').append(variant)
                }
                out.add(
                    HadithReferenceEntity(
                        hadithId = null,
                        stableId = missingStable,
                        collectionId = collectionId,
                        schemeId = scheme,
                        referenceNumber = number,
                        variantSuffix = variant,
                        bookNumber = 0,
                        hadithInBook = 0,
                        canonicalUrl = null,
                        isPrimary = false,
                        reviewStatus = reason
                    )
                )
            }
        }
        reader.endArray()
    }

    private fun parseSingleRecord(
        reader: JsonReader,
        collectionId: String,
        titleEnglish: String,
        defaultScheme: String,
        sourceRevision: String,
        archiveSha256: String,
        licenseStatus: String,
        translationPrefix: String,
        seenRefKeys: HashSet<String>,
        outHadiths: MutableList<HadithEntity>,
        outRefs: MutableList<HadithReferenceEntity>,
        outTranslations: MutableList<HadithTranslationEntity>,
        outGrades: MutableList<HadithGradeEntity>
    ) {
        var id = 0L
        var stableId = ""
        var hadithNumber = 0
        var chapterIdRaw = ""
        var chapterTitle = ""
        var hadithInBook = 0
        var arabic = ""
        var english = ""
        var indonesian = ""
        var canonicalUrl: String? = null
        var contentSha = ""
        var verificationStatus = STATUS_VERIFIED
        var isComplete = true

        val parsedGrades = mutableListOf<Pair<String, String>>()
        val parsedRefs = mutableListOf<RawRef>()

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "id" -> id = reader.nextLong()
                "stable_id" -> stableId = reader.nextStringOrEmpty()
                "hadith_number" -> hadithNumber = reader.nextInt()
                "chapter_id" -> chapterIdRaw = reader.nextStringOrEmpty()
                "chapter_title" -> chapterTitle = reader.nextStringOrEmpty()
                "hadith_in_book" -> hadithInBook = reader.nextInt()
                "arabic" -> arabic = reader.nextStringOrEmpty()
                "english" -> english = reader.nextStringOrEmpty()
                "indonesian" -> indonesian = reader.nextStringOrEmpty()
                "canonical_url" -> canonicalUrl = reader.nextNullableString()
                "content_sha256" -> contentSha = reader.nextStringOrEmpty()
                "verification_status" -> verificationStatus = reader.nextStringOrEmpty().ifBlank { STATUS_VERIFIED }
                "is_complete" -> isComplete = reader.nextBoolean()
                "grades" -> {
                    reader.beginArray()
                    while (reader.hasNext()) {
                        var authority = ""
                        var grade = ""
                        reader.beginObject()
                        while (reader.hasNext()) {
                            when (reader.nextName()) {
                                "authority" -> authority = reader.nextStringOrEmpty().trim()
                                "grade" -> grade = reader.nextStringOrEmpty().trim()
                                else -> reader.skipValue()
                            }
                        }
                        reader.endObject()
                        if (authority.isNotBlank() && grade.isNotBlank()) {
                            parsedGrades.add(authority to grade)
                        }
                    }
                    reader.endArray()
                }
                "references" -> {
                    reader.beginArray()
                    while (reader.hasNext()) {
                        var scheme = defaultScheme
                        var refNum = 0
                        var variant = ""
                        var book = 0
                        var hInBook = 0
                        var url: String? = null
                        reader.beginObject()
                        while (reader.hasNext()) {
                            when (reader.nextName()) {
                                "scheme" -> scheme = reader.nextStringOrEmpty()
                                "number" -> refNum = reader.nextInt()
                                "variant" -> variant = reader.nextStringOrEmpty()
                                "book" -> book = reader.nextInt()
                                "hadith" -> hInBook = reader.nextInt()
                                "url" -> url = reader.nextNullableString()
                                else -> reader.skipValue()
                            }
                        }
                        reader.endObject()
                        parsedRefs.add(RawRef(scheme, refNum, variant, book, hInBook, url))
                    }
                    reader.endArray()
                }
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        val chapterId = chapterIdRaw.takeIf { it.isNotBlank() && it != "0" }
        val bookNumber = chapterId?.toIntOrNull() ?: 0
        val gradeSummary = parsedGrades.joinToString("; ") { "${it.first}: ${it.second}" }.ifBlank { null }
        parsedGrades.forEach { (authority, gradeValue) ->
            outGrades.add(
                HadithGradeEntity(
                    hadithId = id,
                    stableId = stableId,
                    authority = authority,
                    grade = gradeValue
                )
            )
        }

        val schemeTitleTag = when (defaultScheme) {
            SCHEME_SUNNAH -> "Sunnah.com"
            SCHEME_AR_RISALAH -> "Ar-Risalah"
            else -> "Al-Alamiyah"
        }
        val inBookSuffix = if (bookNumber > 0 && hadithInBook > 0) {
            " ($schemeTitleTag • Kitab $bookNumber, Hadis $hadithInBook)"
        } else {
            " ($schemeTitleTag)"
        }
        val refLabel = "$titleEnglish no. $hadithNumber$inBookSuffix"

        outHadiths.add(
            HadithEntity(
                id = id,
                collectionId = collectionId,
                hadithNumber = hadithNumber,
                title = chapterTitle.ifBlank { titleEnglish },
                textArabic = arabic,
                translationId = indonesian,
                translationEn = english,
                reference = refLabel,
                chapterId = chapterId,
                sourceRevision = sourceRevision,
                sourceSha256 = archiveSha256,
                licenseStatus = licenseStatus,
                grade = gradeSummary,
                language = if (indonesian.isNotBlank()) "id" else "en",
                isComplete = isComplete,
                stableId = stableId,
                schemeId = defaultScheme,
                canonicalUrl = canonicalUrl,
                contentChecksum = contentSha,
                verificationStatus = verificationStatus
            )
        )

        var hasArchiveRef = false
        for (r in parsedRefs) {
            if (r.scheme == SCHEME_ARCHIVE_GADING) hasArchiveRef = true
            val uniqueKey = "$collectionId|${r.scheme}|${r.number}|${r.variant}"
            if (seenRefKeys.add(uniqueKey)) {
                outRefs.add(
                    HadithReferenceEntity(
                        hadithId = id,
                        stableId = stableId,
                        collectionId = collectionId,
                        schemeId = r.scheme,
                        referenceNumber = r.number,
                        variantSuffix = r.variant,
                        bookNumber = r.book,
                        hadithInBook = r.hadith,
                        canonicalUrl = r.url,
                        isPrimary = r.scheme == defaultScheme,
                        reviewStatus = "verified"
                    )
                )
            }
        }

        if (english.isNotBlank()) {
            outTranslations.add(
                HadithTranslationEntity(
                    hadithId = id,
                    stableId = stableId,
                    collectionId = collectionId,
                    language = "en",
                    editionId = "$translationPrefix-eng-$collectionId",
                    text = english,
                    sourceRevision = sourceRevision,
                    licenseStatus = licenseStatus
                )
            )
        }
        if (indonesian.isNotBlank()) {
            outTranslations.add(
                HadithTranslationEntity(
                    hadithId = id,
                    stableId = stableId,
                    collectionId = collectionId,
                    language = "id",
                    editionId = if (hasArchiveRef && defaultScheme == SCHEME_SUNNAH) {
                        "gadingnst-ind-crosswalk"
                    } else {
                        "$translationPrefix-ind-$collectionId"
                    },
                    text = indonesian,
                    sourceRevision = sourceRevision,
                    licenseStatus = licenseStatus
                )
            )
        }
    }

    private suspend fun recordFailure(
        sourceId: String,
        sourceUri: String,
        revision: String,
        archiveSha256: String,
        licenseId: String,
        activeCount: Int,
        missingCount: Int,
        status: String,
        reason: String
    ): CanonicalSyncResult.Failed {
        database.hadithDao().insertSourceManifest(
            HadithSourceManifestEntity(
                sourceId = sourceId,
                sourceUri = sourceUri,
                revision = revision,
                archiveSha256 = archiveSha256,
                licenseId = licenseId,
                activeRecordCount = activeCount,
                missingSlotCount = missingCount,
                status = status,
                updatedAtEpochMs = System.currentTimeMillis()
            )
        )
        return CanonicalSyncResult.Failed(status, reason)
    }

    private fun JsonReader.nextStringOrEmpty(): String =
        if (peek() == JsonToken.NULL) {
            nextNull()
            ""
        } else {
            nextString()
        }

    private fun JsonReader.nextNullableString(): String? =
        if (peek() == JsonToken.NULL) {
            nextNull()
            null
        } else {
            nextString().takeIf { it.isNotBlank() && it != "null" }
        }

    private data class RawRef(
        val scheme: String,
        val number: Int,
        val variant: String,
        val book: Int,
        val hadith: Int,
        val url: String?
    )

    private fun copyAssetAndDigest(assetPath: String, destination: File): Pair<Long, String> {
        val digest = MessageDigest.getInstance("SHA-256")
        var totalBytes = 0L
        context.assets.open(assetPath).use { input ->
            destination.outputStream().use { output ->
                val buffer = ByteArray(16_384)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    digest.update(buffer, 0, read)
                    output.write(buffer, 0, read)
                    totalBytes += read
                }
            }
        }
        return totalBytes to digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun sha256StreamHex(input: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(16_384)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun licenseStatusForLicenseName(licenseName: String): String =
        if (licenseName.equals("MIT", ignoreCase = true)) CANONICAL_MIT_LICENSE_STATUS else CANONICAL_LICENSE_STATUS

    private fun schemeDisplayLabel(schemeId: String): String = when (schemeId) {
        SCHEME_SUNNAH -> "No. Sunnah.com"
        SCHEME_AR_RISALAH -> "No. Ar-Risalah"
        SCHEME_ARCHIVE_GADING -> "No. Arsip"
        else -> schemeId
    }

    companion object {
        private const val STREAM_BATCH_SIZE = 400
        const val MANIFEST_ASSET_PATH = "hadith/corpus-manifest.json"
        const val BUNDLE_ASSET_PATH = "hadith/sunnah-canonical-bundle.zip"
        const val CANONICAL_SOURCE_ID = "fawazahmed0-hadith-api"
        const val CANONICAL_LICENSE_STATUS = "canonical_unlicense"
        const val CANONICAL_MIT_LICENSE_STATUS = "canonical_mit"
        val CANONICAL_ACCEPTED_LICENSES = setOf(CANONICAL_LICENSE_STATUS, CANONICAL_MIT_LICENSE_STATUS)

        const val SCHEME_SUNNAH = "sunnah"
        const val SCHEME_SUNNAH_ORDINAL = "sunnah_ordinal"
        const val SCHEME_AR_RISALAH = "ar_risalah"
        const val SCHEME_ARCHIVE_GADING = "archive_gading"
        const val REVIEW_MISSING_IN_SOURCE = "missing_in_source"

        const val STATUS_VERIFIED = "VERIFIED"
        const val STATUS_PARTIAL = "PARTIAL"
        const val STATUS_CORRUPT = "CORRUPT"
        const val STATUS_NOT_INSTALLED = "NOT_INSTALLED"
        const val STATUS_AVAILABLE = "AVAILABLE"

        val CANONICAL_COLLECTION_IDS = setOf(
            "bukhari", "muslim", "abudawud", "tirmidhi", "nasai", "ibnmajah", "malik", "ahmad", "darimi"
        )

        val ARCHIVE_ONLY_META: Map<String, Pair<String, String>> = emptyMap()
    }
}
