package com.quranplus.app.features.hadith.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.quranplus.app.core.database.QuranDatabase
import com.quranplus.app.core.database.entity.HadithChapterEntity
import com.quranplus.app.core.database.entity.HadithCollectionEntity
import com.quranplus.app.core.database.entity.HadithEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class HadithImportSummary(
    val collectionId: String,
    val title: String,
    val recordCount: Int
)

/** Imports a real Hadist JSON source selected through SAF or extracted from the bundle. */
class HadithReferenceImporter(
    private val context: Context,
    private val database: QuranDatabase
) {
    suspend fun import(uri: Uri): HadithImportSummary? = withContext(Dispatchers.IO) {
        val displayName = queryDisplayName(uri) ?: uri.lastPathSegment ?: return@withContext null
        val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: return@withContext null
        importJson(json, displayName)
    }

    suspend fun importFile(file: File, displayName: String = file.name): HadithImportSummary? =
        withContext(Dispatchers.IO) {
            if (!file.isFile) return@withContext null
            importJson(file.readText(Charsets.UTF_8), displayName)
        }

    private suspend fun importJson(json: String, displayName: String): HadithImportSummary? {
        val parsed = parseCollection(json, displayName) ?: return null
        if (parsed.records.isEmpty()) return null

        val dao = database.hadithDao()
        dao.deleteHadithsForCollection(parsed.collectionId)
        dao.deleteChaptersForCollection(parsed.collectionId)
        dao.insertCollections(
            listOf(
                HadithCollectionEntity(
                    id = parsed.collectionId,
                    titleArabic = parsed.titleArabic,
                    titleEnglish = parsed.title,
                    sourceRevision = parsed.sourceRevision,
                    sourceSha256 = "",
                    licenseStatus = "reference",
                    gradeStatus = "not_provided",
                    recordCount = parsed.records.size,
                    chapterCount = parsed.chapters.size,
                    isComplete = parsed.isComplete,
                    bundleAllowed = parsed.isBundle
                )
            )
        )
        dao.insertChapters(parsed.chapters)
        parsed.records.chunked(BATCH_SIZE).forEach { batch -> dao.insertHadiths(batch) }
        return HadithImportSummary(parsed.collectionId, parsed.title, parsed.records.size)
    }

    private fun parseCollection(json: String, displayName: String): ParsedCollection? {
        val array = runCatching { JSONArray(json) }.getOrNull()
        if (array != null) return parseIndonesianBundle(array, displayName)

        val document = runCatching { JSONObject(json) }.getOrNull() ?: return null
        val hadiths = document.optJSONArray("hadiths") ?: return null
        val chapters = document.optJSONArray("chapters") ?: JSONArray()
        val metadata = document.optJSONObject("metadata")
        val collectionId = collectionId(displayName, document)
        val titleArabic = metadata?.optJSONObject("arabic")?.optString("title").orEmpty()
        val title = metadata?.optJSONObject("english")?.optString("title")
            .orEmpty().ifBlank { displayTitle(collectionId) }
        val records = buildReferenceRecords(collectionId, title, hadiths)
        return ParsedCollection(
            collectionId = collectionId,
            title = title,
            titleArabic = titleArabic,
            records = records,
            chapters = buildChapters(collectionId, chapters),
            sourceRevision = "hadith-json-reference",
            isComplete = records.size == hadiths.length(),
            isBundle = false
        )
    }

    private fun parseIndonesianBundle(
        hadiths: JSONArray,
        displayName: String
    ): ParsedCollection? {
        val collectionId = collectionIdFromFileName(displayName) ?: return null
        val title = displayTitle(collectionId)
        val records = buildList {
            for (index in 0 until hadiths.length()) {
                val record = hadiths.optJSONObject(index) ?: continue
                val number = record.optInt("number")
                val arabic = record.optString("arab")
                val translation = record.optString("id")
                if (number <= 0 || arabic.isBlank() || translation.isBlank()) continue
                add(
                    HadithEntity(
                        id = stableBundleId(collectionId, number),
                        collectionId = collectionId,
                        hadithNumber = number,
                        title = title,
                        textArabic = arabic,
                        translationId = translation,
                        translationEn = "",
                        reference = "$title no. $number",
                        chapterId = null,
                        sourceRevision = HADITH_BUNDLE_REVISION,
                        sourceSha256 = "",
                        licenseStatus = "reference",
                        grade = null,
                        language = "id",
                        isComplete = true
                    )
                )
            }
        }.distinctBy { it.hadithNumber }.sortedBy { it.hadithNumber }
        return ParsedCollection(
            collectionId = collectionId,
            title = title,
            titleArabic = "",
            records = records,
            chapters = emptyList(),
            sourceRevision = HADITH_BUNDLE_REVISION,
            isComplete = records.size == hadiths.length(),
            isBundle = true
        )
    }

    private fun buildReferenceRecords(
        collectionId: String,
        title: String,
        hadiths: JSONArray
    ): List<HadithEntity> = buildList {
        for (index in 0 until hadiths.length()) {
            val record = hadiths.optJSONObject(index) ?: continue
            val id = record.optLong("id")
            val hadithNumber = record.optInt("idInBook")
            val arabic = record.optString("arabic")
            val english = record.optJSONObject("english")
            val narrator = english?.optString("narrator").orEmpty()
            val translation = english?.optString("text").orEmpty()
            val translationId = indonesianTranslation(record)
            if (id <= 0L || hadithNumber <= 0 || arabic.isBlank()) continue
            add(
                HadithEntity(
                    id = id,
                    collectionId = collectionId,
                    hadithNumber = hadithNumber,
                    title = title,
                    textArabic = arabic,
                    translationId = translationId,
                    translationEn = listOf(narrator, translation)
                        .filter(String::isNotBlank)
                        .joinToString("\n"),
                    reference = "$title no. $hadithNumber",
                    chapterId = record.optInt("chapterId").toString(),
                    sourceRevision = "hadith-json-reference",
                    sourceSha256 = "",
                    licenseStatus = "reference",
                    grade = null,
                    language = if (translationId.isBlank()) "en" else "id",
                    isComplete = translation.isNotBlank() || translationId.isNotBlank()
                )
            )
        }
    }

    private fun indonesianTranslation(record: JSONObject): String = listOf(
        record.optString("translation_id"),
        record.optString("translationId"),
        record.optString("indonesian"),
        record.optString("indonesia"),
        record.optString("terjemahan")
    ).firstOrNull(String::isNotBlank).orEmpty()

    private fun collectionId(displayName: String, document: JSONObject): String =
        collectionIdFromFileName(displayName)
            ?: document.optInt("id").takeIf { it > 0 }?.toString()
            ?: displayName.substringAfterLast('/').substringAfterLast('\\')
                .substringBeforeLast('.')
                .lowercase()

    private fun queryDisplayName(uri: Uri): String? {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getString(0)
        }
        return null
    }

    private fun buildChapters(
        collectionId: String,
        chapters: JSONArray
    ): List<HadithChapterEntity> = buildList {
        for (index in 0 until chapters.length()) {
            val chapter = chapters.optJSONObject(index) ?: continue
            val id = chapter.optInt("id")
            if (id < 0) continue
            add(
                HadithChapterEntity(
                    collectionId = collectionId,
                    chapterId = id.toString(),
                    chapterNumber = index + 1,
                    titleArabic = chapter.optString("arabic"),
                    titleEnglish = chapter.optString("english")
                )
            )
        }
    }

    private data class ParsedCollection(
        val collectionId: String,
        val title: String,
        val titleArabic: String,
        val records: List<HadithEntity>,
        val chapters: List<HadithChapterEntity>,
        val sourceRevision: String,
        val isComplete: Boolean,
        val isBundle: Boolean
    )

    companion object {
        const val HADITH_BUNDLE_REVISION = "gadingnst/hadith-api-master"
        val BUNDLE_BOOK_NAMES = setOf(
            "abu-daud.json",
            "ahmad.json",
            "bukhari.json",
            "darimi.json",
            "ibnu-majah.json",
            "malik.json",
            "muslim.json",
            "nasai.json",
            "tirmidzi.json"
        )

        fun collectionIdFromFileName(displayName: String): String? {
            val filename = displayName.substringAfterLast('/').substringAfterLast('\\').lowercase()
            val basename = filename.substringBeforeLast('.')
            return when (basename) {
                "abu-daud" -> "abudawud"
                "ibnu-majah" -> "ibnmajah"
                "tirmidzi" -> "tirmidhi"
                in setOf("ahmad", "bukhari", "darimi", "malik", "muslim", "nasai") -> basename
                else -> null
            }
        }

        fun displayTitle(collectionId: String): String = when (collectionId) {
            "abudawud" -> "Sunan Abu Dawud"
            "tirmidhi" -> "Jami' al-Tirmidhi"
            "ibnmajah" -> "Sunan Ibn Majah"
            "bukhari" -> "Sahih al-Bukhari"
            "muslim" -> "Sahih Muslim"
            "nasai" -> "Sunan al-Nasa'i"
            "ahmad" -> "Musnad Ahmad"
            "darimi" -> "Sunan al-Darimi"
            "malik" -> "Muwatta Malik"
            else -> collectionId
        }

        private fun stableBundleId(collectionId: String, number: Int): Long {
            val knownIds = listOf(
                "bukhari", "muslim", "abudawud", "tirmidhi", "nasai",
                "ibnmajah", "ahmad", "darimi", "malik"
            )
            val collectionKey = knownIds.indexOf(collectionId).takeIf { it >= 0 }
                ?.plus(1)
                ?.toLong()
                ?: (1000L + (collectionId.hashCode().toUInt().toLong() % 9000L))
            return collectionKey * 1_000_000L + number
        }

        private const val BATCH_SIZE = 250
    }
}
