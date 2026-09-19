package com.quranplus.app.features.hadith.data

import com.quranplus.app.core.database.dao.HadithDao
import com.quranplus.app.features.hadith.domain.HadithCollection
import com.quranplus.app.features.hadith.domain.HadithChapter
import com.quranplus.app.features.hadith.domain.HadithRecord
import com.quranplus.app.features.hadith.domain.HadithRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import androidx.sqlite.db.SimpleSQLiteQuery

class HadithRepositoryImpl(
    private val dao: HadithDao
) : HadithRepository {
    override fun getCollections(): Flow<List<HadithCollection>> =
        combine(dao.getCollections(), dao.getCollectionIds()) { collections, idsWithContent ->
            collections.map { collection ->
                HadithCollection(
                    id = collection.id,
                    title = displayTitle(collection.id, collection.titleEnglish),
                    count = collection.recordCount,
                    titleArabic = collection.titleArabic,
                    hasLocalContent = collection.id in idsWithContent
                )
            }
        }

    override fun getChapters(collectionId: String): Flow<List<HadithChapter>> =
        dao.getChapters(collectionId).map { chapters ->
            chapters.map { chapter ->
                HadithChapter(
                    collectionId = chapter.collectionId,
                    id = chapter.chapterId,
                    number = chapter.chapterNumber,
                    titleArabic = chapter.titleArabic,
                    titleEnglish = chapter.titleEnglish
                )
            }
        }

    override suspend fun search(collectionId: String?, query: String): List<HadithRecord> {
        val trimmed = query.trim()
        val cleanNumberStr = trimmed.replace(Regex("(?i)^(hadits?|no\\.?|nomor)\\s*"), "").trim()
        val numberQuery = cleanNumberStr.toIntOrNull()
            ?: Regex("""\b\d+\b""").find(trimmed)?.value?.toIntOrNull()

        val searchQuery = if (cleanNumberStr.isNotBlank()) cleanNumberStr else trimmed

        // Hadist yang ditampilkan di katalog harus berasal dari bundle lokal
        // yang telah diverifikasi. Hasil internet tidak dipersist sebagai
        // corpus karena dapat terlihat seperti dalil terverifikasi.
        val localEntities = when {
            searchQuery.isBlank() && collectionId != null ->
                dao.getHadithsByCollection(collectionId).first()
            numberQuery != null ->
                dao.searchByNumber(collectionId, numberQuery)
            else -> searchQuery.toFtsExpression()?.let { expression ->
                dao.searchFts(
                    SimpleSQLiteQuery(
                        """
                        SELECT h.* FROM hadiths AS h
                        JOIN hadiths_fts5 ON h.id = hadiths_fts5.rowid
                        WHERE (? IS NULL OR h.collection_id = ?)
                          AND hadiths_fts5 MATCH ?
                        ORDER BY bm25(hadiths_fts5), h.hadith_number ASC
                        LIMIT ?
                        """.trimIndent(),
                        arrayOf<Any?>(collectionId, collectionId, expression, MAX_RESULTS)
                    )
                )
            }.orEmpty()
        }
        return localEntities.map { it.toRecord() }
    }

    private fun String.toFtsExpression(): String? {
        val tokens = Regex("[\\p{L}\\p{N}]+")
            .findAll(lowercase())
            .map { it.value }
            .filter { it.length >= 2 }
            .distinct()
            .take(MAX_QUERY_TOKENS)
            .toList()
        if (tokens.isEmpty()) return null
        return tokens.joinToString(" AND ") { token ->
            "\"${token.replace("\"", "\"\"")}\"*"
        }
    }

    private fun com.quranplus.app.core.database.entity.HadithEntity.toRecord(): HadithRecord =
        HadithRecord(
            id = id,
            collectionId = collectionId,
            hadithNumber = hadithNumber,
            title = title,
            textArabic = textArabic,
            translationId = translationId,
            translationEn = translationEn,
            reference = reference,
            chapterId = chapterId
        )

    private fun displayTitle(id: String, sourceTitle: String): String = when (id) {
        "abudawud" -> "Sunan Abu Dawud"
        else -> sourceTitle
    }

    private companion object {
        const val MAX_RESULTS = 100
        const val MAX_QUERY_TOKENS = 12
    }
}
