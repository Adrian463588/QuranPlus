package com.quranplus.app.features.quran.domain

import kotlinx.coroutines.flow.Flow

class GetSurahListUseCase(private val repository: QuranRepository) {
    operator fun invoke(): Flow<List<Surah>> = repository.getAllSurahs()
}

class GetSurahDetailUseCase(private val repository: QuranRepository) {
    suspend operator fun invoke(surahNumber: Int): Surah? = repository.getSurah(surahNumber)
}

class GetAyahsBySurahUseCase(private val repository: QuranRepository) {
    operator fun invoke(surahNumber: Int): Flow<List<Ayah>> = repository.getAyahsBySurah(surahNumber)
}

class SearchQuranUseCase(private val repository: QuranRepository) {
    suspend operator fun invoke(query: String): List<Ayah> = repository.searchAyahs(query)
}

class ToggleBookmarkUseCase(private val repository: QuranRepository) {
    suspend operator fun invoke(
        surahNumber: Int,
        surahName: String,
        ayahNumber: Int,
        textArabic: String,
        translation: String,
        note: String? = null
    ) = repository.toggleBookmark(surahNumber, surahName, ayahNumber, textArabic, translation, note)
}

class GetBookmarksUseCase(private val repository: QuranRepository) {
    operator fun invoke(): Flow<List<Bookmark>> = repository.getAllBookmarks()
}

class DeleteBookmarkUseCase(private val repository: QuranRepository) {
    suspend operator fun invoke(id: Long) = repository.deleteBookmark(id)
}

class SaveLastReadUseCase(private val repository: QuranRepository) {
    suspend operator fun invoke(surahNumber: Int, surahName: String, ayahNumber: Int) =
        repository.saveLastRead(surahNumber, surahName, ayahNumber)
}

class GetLastReadUseCase(private val repository: QuranRepository) {
    operator fun invoke(): Flow<LastRead?> = repository.getLastRead()
}
