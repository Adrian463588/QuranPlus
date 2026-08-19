package com.quranplus.app.features.quran.data

import com.quranplus.app.core.database.dao.BookmarkDao
import com.quranplus.app.core.database.dao.LastReadDao
import com.quranplus.app.core.database.dao.QuranDao
import com.quranplus.app.core.database.entity.BookmarkEntity
import com.quranplus.app.core.database.entity.LastReadEntity
import com.quranplus.app.features.quran.domain.Ayah
import com.quranplus.app.features.quran.domain.Bookmark
import com.quranplus.app.features.quran.domain.BookmarkSort
import com.quranplus.app.features.quran.domain.LastRead
import com.quranplus.app.features.quran.domain.QuranRepository
import com.quranplus.app.features.quran.domain.Surah
import androidx.sqlite.db.SimpleSQLiteQuery
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class QuranRepositoryImpl(
    private val quranDao: QuranDao,
    private val bookmarkDao: BookmarkDao,
    private val lastReadDao: LastReadDao
) : QuranRepository {

    override fun getAllSurahs(): Flow<List<Surah>> {
        return quranDao.getAllSurahs().map { list ->
            list.map { entity ->
                Surah(
                    number = entity.number,
                    nameArabic = entity.nameArabic,
                    nameLatin = entity.nameLatin,
                    nameEnglish = entity.nameEnglish,
                    revelationType = entity.revelationType,
                    ayahCount = entity.ayahCount
                )
            }
        }
    }

    override suspend fun getSurah(surahNumber: Int): Surah? {
        val entity = quranDao.getSurahByNumber(surahNumber) ?: return null
        return Surah(
            number = entity.number,
            nameArabic = entity.nameArabic,
            nameLatin = entity.nameLatin,
            nameEnglish = entity.nameEnglish,
            revelationType = entity.revelationType,
            ayahCount = entity.ayahCount
        )
    }

    override fun getAyahsBySurah(surahNumber: Int): Flow<List<Ayah>> {
        val ayahsFlow = quranDao.getAyahsBySurah(surahNumber)
        val bookmarksFlow = bookmarkDao.getAllBookmarks()

        return combine(ayahsFlow, bookmarksFlow) { ayahs, bookmarks ->
            val bookmarkedAyahs = bookmarks.filter { it.surahId == surahNumber }.map { it.ayahNumber }.toSet()
            ayahs.map { entity ->
                Ayah(
                    id = entity.id,
                    surahNumber = entity.surahId,
                    ayahNumber = entity.ayahNumber,
                    textArabic = entity.textArabic,
                    transliteration = entity.transliteration,
                    translationId = entity.translationId,
                    translationEn = entity.translationEn,
                    juz = entity.juz,
                    page = entity.page,
                    tajwidTags = entity.tajwidTags,
                    isBookmarked = bookmarkedAyahs.contains(entity.ayahNumber)
                )
            }
        }
    }

    override suspend fun getFirstAyahByPage(page: Int): Ayah? {
        require(page in 1..604) { "Halaman Quran harus berada di antara 1 dan 604" }
        return quranDao.getFirstAyahByPage(page)?.toDomainAyah()
    }

    override suspend fun getFirstAyahByJuz(juz: Int): Ayah? {
        require(juz in 1..30) { "Juz Quran harus berada di antara 1 dan 30" }
        return quranDao.getFirstAyahByJuz(juz)?.toDomainAyah()
    }

    private suspend fun com.quranplus.app.core.database.entity.AyahEntity.toDomainAyah(): Ayah {
        val surah = quranDao.getSurahByNumber(surahId)
            ?: throw IllegalStateException("Surah $surahId tidak tersedia untuk posisi Quran")
        return Ayah(
            id = id,
            surahNumber = surahId,
            surahName = surah.nameLatin,
            ayahNumber = ayahNumber,
            textArabic = textArabic,
            transliteration = transliteration,
            translationId = translationId,
            translationEn = translationEn,
            juz = juz,
            page = page,
            tajwidTags = tajwidTags,
            isBookmarked = bookmarkDao.getBookmark(surahId, ayahNumber) != null
        )
    }

    override suspend fun searchAyahs(query: String, surahNumber: Int?): List<Ayah> {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return emptyList()
        require(surahNumber == null || surahNumber in 1..114) {
            "Filter surah tidak valid"
        }

        val ftsExpression = cleanQuery
            .split(Regex("\\s+"))
            .filter(String::isNotBlank)
            .joinToString(" AND ") { token ->
                "\"${token.replace("\"", "\"\"")}\"*"
            }
        val filterClause = if (surahNumber == null) "" else "AND a.surah_id = ?"
        val queryArgs = if (surahNumber == null) {
            arrayOf<Any>(ftsExpression, 50)
        } else {
            arrayOf<Any>(ftsExpression, surahNumber, 50)
        }
        val results = quranDao.searchAyahsFts(
            SimpleSQLiteQuery(
                """
                SELECT a.* FROM ayahs AS a
                JOIN ayahs_fts5 ON a.id = ayahs_fts5.rowid
                JOIN surahs AS s ON s.number = a.surah_id
                WHERE ayahs_fts5 MATCH ? $filterClause
                ORDER BY a.surah_id ASC, a.ayah_number ASC
                LIMIT ?
                """.trimIndent(),
                queryArgs
            )
        )
        val surahNames = results
            .map { it.surahId }
            .distinct()
            .associateWith { id ->
                quranDao.getSurahByNumber(id)?.nameLatin
                    ?: throw IllegalStateException("Nama surah $id tidak tersedia")
            }

        return results.map { entity ->
            Ayah(
                id = entity.id,
                surahNumber = entity.surahId,
                surahName = surahNames.getValue(entity.surahId),
                ayahNumber = entity.ayahNumber,
                textArabic = entity.textArabic,
                transliteration = entity.transliteration,
                translationId = entity.translationId,
                translationEn = entity.translationEn,
                juz = entity.juz,
                page = entity.page,
                tajwidTags = entity.tajwidTags
            )
        }
    }

    override fun getAllBookmarks(sort: BookmarkSort): Flow<List<Bookmark>> {
        val source = when (sort) {
            BookmarkSort.NEWEST -> bookmarkDao.getAllBookmarks()
            BookmarkSort.SURAH -> bookmarkDao.getAllBookmarksBySurah()
        }
        return source.map { list ->
            list.map { entity ->
                Bookmark(
                    id = entity.id,
                    surahNumber = entity.surahId,
                    surahName = entity.surahName,
                    ayahNumber = entity.ayahNumber,
                    ayahTextArabic = entity.ayahTextArabic,
                    ayahTranslation = entity.ayahTranslation,
                    note = entity.note,
                    timestamp = entity.timestamp
                )
            }
        }
    }

    override fun isAyahBookmarked(surahNumber: Int, ayahNumber: Int): Flow<Boolean> {
        return bookmarkDao.isBookmarked(surahNumber, ayahNumber)
    }

    override suspend fun toggleBookmark(
        surahNumber: Int,
        surahName: String,
        ayahNumber: Int,
        textArabic: String,
        translation: String,
        note: String?
    ) {
        val existing = bookmarkDao.getBookmark(surahNumber, ayahNumber)
        if (existing != null) {
            bookmarkDao.deleteBookmark(existing)
        } else {
            bookmarkDao.insertBookmark(
                BookmarkEntity(
                    surahId = surahNumber,
                    surahName = surahName,
                    ayahNumber = ayahNumber,
                    ayahTextArabic = textArabic,
                    ayahTranslation = translation,
                    note = note
                )
            )
        }
    }

    override suspend fun deleteBookmark(id: Long) {
        bookmarkDao.deleteBookmarkById(id)
    }

    override suspend fun restoreBookmark(bookmark: Bookmark) {
        bookmarkDao.insertBookmark(
            BookmarkEntity(
                id = bookmark.id,
                surahId = bookmark.surahNumber,
                surahName = bookmark.surahName,
                ayahNumber = bookmark.ayahNumber,
                ayahTextArabic = bookmark.ayahTextArabic,
                ayahTranslation = bookmark.ayahTranslation,
                note = bookmark.note,
                timestamp = bookmark.timestamp
            )
        )
    }

    override suspend fun updateBookmarkNote(id: Long, note: String?) {
        bookmarkDao.updateNote(id, note?.takeIf(String::isNotBlank))
    }

    override fun getLastRead(): Flow<LastRead?> {
        return lastReadDao.getLastRead().map { entity ->
            entity?.let {
                LastRead(
                    surahNumber = it.surahId,
                    surahName = it.surahName,
                    ayahNumber = it.ayahNumber,
                    juz = it.juz,
                    page = it.page,
                    timestamp = it.timestamp
                )
            }
        }
    }

    override suspend fun saveLastRead(
        surahNumber: Int,
        surahName: String,
        ayahNumber: Int,
        juz: Int,
        page: Int
    ) {
        require(surahNumber in 1..114)
        require(ayahNumber > 0)
        require(juz > 0)
        require(page > 0)
        lastReadDao.saveLastRead(
            LastReadEntity(
                id = 1,
                surahId = surahNumber,
                surahName = surahName,
                ayahNumber = ayahNumber,
                juz = juz,
                page = page,
                timestamp = System.currentTimeMillis()
            )
        )
    }
}
