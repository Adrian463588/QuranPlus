package com.quranplus.shared.features.quran.domain

import kotlinx.coroutines.flow.Flow

enum class BookmarkSort { NEWEST, SURAH }

interface QuranRepository {
    fun getAllSurahs(): Flow<List<Surah>>
    suspend fun getSurah(surahNumber: Int): Surah?
    fun getAyahsBySurah(surahNumber: Int): Flow<List<Ayah>>
    suspend fun getFirstAyahByPage(page: Int): Ayah?
    suspend fun getFirstAyahByJuz(juz: Int): Ayah?
    suspend fun searchAyahs(
        query: String,
        filter: QuranSearchFilter = QuranSearchFilter()
    ): List<Ayah>
    fun getAllBookmarks(sort: BookmarkSort = BookmarkSort.NEWEST): Flow<List<Bookmark>>
    fun isAyahBookmarked(surahNumber: Int, ayahNumber: Int): Flow<Boolean>
    suspend fun toggleBookmark(
        surahNumber: Int,
        surahName: String,
        ayahNumber: Int,
        textArabic: String,
        translation: String,
        note: String? = null
    )
    suspend fun deleteBookmark(id: Long)
    suspend fun restoreBookmark(bookmark: Bookmark)
    suspend fun updateBookmarkNote(id: Long, note: String?)
    fun getLastRead(): Flow<LastRead?>
    suspend fun saveLastRead(
        surahNumber: Int,
        surahName: String,
        ayahNumber: Int,
        juz: Int = 1,
        page: Int = 1
    )
}
