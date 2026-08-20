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
import org.json.JSONObject

data class HadithImportSummary(
    val collectionId: String,
    val title: String,
    val recordCount: Int
)

/** Imports the actual hadith-json collection selected by the user through SAF. */
class HadithReferenceImporter(
    private val context: Context,
    private val database: QuranDatabase
) {
    suspend fun import(uri: Uri): HadithImportSummary? = withContext(Dispatchers.IO) {
        val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: return@withContext null
        val document = runCatching { JSONObject(json) }.getOrNull() ?: return@withContext null
        val hadiths = document.optJSONArray("hadiths") ?: return@withContext null
        val chapters = document.optJSONArray("chapters") ?: return@withContext null
        val metadata = document.optJSONObject("metadata")
        val collectionId = collectionId(uri, document)
        val titleArabic = metadata?.optJSONObject("arabic")?.optString("title").orEmpty()
        val titleEnglish = metadata?.optJSONObject("english")?.optString("title")
            .orEmpty().ifBlank { collectionId }
        val records = buildRecords(collectionId, titleEnglish, hadiths)
        if (records.isEmpty()) return@withContext null
        val chapterEntities = buildChapters(collectionId, chapters)

        database.hadithDao().insertCollections(
            listOf(
                HadithCollectionEntity(
                    id = collectionId,
                    titleArabic = titleArabic,
                    titleEnglish = titleEnglish,
                    sourceRevision = "",
                    sourceSha256 = "",
                    licenseStatus = "reference",
                    gradeStatus = "not_provided",
                    recordCount = records.size,
                    chapterCount = chapterEntities.size,
                    isComplete = records.size == hadiths.length(),
                    bundleAllowed = false
                )
            )
        )
        database.hadithDao().insertChapters(chapterEntities)
        records.chunked(BATCH_SIZE).forEach { database.hadithDao().insertHadiths(it) }
        HadithImportSummary(collectionId, titleEnglish, records.size)
    }

    private fun buildRecords(
        collectionId: String,
        title: String,
        hadiths: org.json.JSONArray
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
                    sourceRevision = "",
                    sourceSha256 = "",
                    licenseStatus = "reference",
                    grade = null,
                    language = "en",
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

    private fun collectionId(uri: Uri, document: JSONObject): String {
        val name = context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
            ?.lowercase()
            .orEmpty()
        val known = listOf(
            "nawawi40", "qudsi40", "shahwaliullah40", "aladab_almufrad",
            "bulugh_almaram", "mishkat_almasabih", "riyad_assalihin",
            "shamail_muhammadiyah", "abudawud", "ahmed", "bukhari", "darimi",
            "ibnmajah", "malik", "muslim", "nasai", "tirmidhi"
        )
        return known.firstOrNull { name.contains(it) }
            ?: document.optInt("id").takeIf { it > 0 }?.toString()
            ?: uri.lastPathSegment?.substringAfterLast('/').orEmpty()
    }

    private fun buildChapters(
        collectionId: String,
        chapters: org.json.JSONArray
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

    private companion object {
        const val BATCH_SIZE = 250
    }
}
