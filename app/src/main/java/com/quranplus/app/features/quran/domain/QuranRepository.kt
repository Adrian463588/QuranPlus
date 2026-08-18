package com.quranplus.app.features.quran.domain

import kotlinx.coroutines.flow.Flow

interface QuranRepository {
    fun getAllSurahs(): Flow<List<Surah>>
    suspend fun getSurah(surahNumber: Int): Surah?
    fun getAyahsBySurah(surahNumber: Int): Flow<List<Ayah>>
    suspend fun searchAyahs(query: String): List<Ayah>
    fun getAllBookmarks(): Flow<List<Bookmark>>
    fun isAyahBookmarked(surahNumber: Int, ayahNumber: Int): Flow<Boolean>
    suspend fun toggleBookmark(surahNumber: Int, surahName: String, ayahNumber: Int, textArabic: String, translation: String, note: String? = null)
    suspend fun deleteBookmark(id: Long)
    fun getLastRead(): Flow<LastRead?>
    suspend fun saveLastRead(surahNumber: Int, surahName: String, ayahNumber: Int)
}
