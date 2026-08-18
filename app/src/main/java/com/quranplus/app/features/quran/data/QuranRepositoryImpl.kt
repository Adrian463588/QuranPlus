package com.quranplus.app.features.quran.data

import com.quranplus.app.core.database.dao.BookmarkDao
import com.quranplus.app.core.database.dao.LastReadDao
import com.quranplus.app.core.database.dao.QuranDao
import com.quranplus.app.core.database.entity.BookmarkEntity
import com.quranplus.app.core.database.entity.LastReadEntity
import com.quranplus.app.features.quran.domain.Ayah
import com.quranplus.app.features.quran.domain.Bookmark
import com.quranplus.app.features.quran.domain.LastRead
import com.quranplus.app.features.quran.domain.QuranRepository
import com.quranplus.app.features.quran.domain.Surah
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

    override suspend fun searchAyahs(query: String): List<Ayah> {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return emptyList()

        val results = runCatching { quranDao.searchAyahsFts(cleanQuery) }
            .getOrElse { quranDao.searchAyahsLike(cleanQuery) }

        return results.map { entity ->
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
                tajwidTags = entity.tajwidTags
            )
        }
    }

    override fun getAllBookmarks(): Flow<List<Bookmark>> {
        return bookmarkDao.getAllBookmarks().map { list ->
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

    override fun getLastRead(): Flow<LastRead?> {
        return lastReadDao.getLastRead().map { entity ->
            entity?.let {
                LastRead(
                    surahNumber = it.surahId,
                    surahName = it.surahName,
                    ayahNumber = it.ayahNumber,
                    timestamp = it.timestamp
                )
            }
        }
    }

    override suspend fun saveLastRead(surahNumber: Int, surahName: String, ayahNumber: Int) {
        lastReadDao.saveLastRead(
            LastReadEntity(
                id = 1,
                surahId = surahNumber,
                surahName = surahName,
                ayahNumber = ayahNumber,
                timestamp = System.currentTimeMillis()
            )
        )
    }
}
