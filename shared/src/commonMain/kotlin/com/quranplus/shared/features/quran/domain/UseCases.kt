package com.quranplus.shared.features.quran.domain

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

class GetFirstAyahByPageUseCase(private val repository: QuranRepository) {
    suspend operator fun invoke(page: Int): Ayah? = repository.getFirstAyahByPage(page)
}

class GetFirstAyahByJuzUseCase(private val repository: QuranRepository) {
    suspend operator fun invoke(juz: Int): Ayah? = repository.getFirstAyahByJuz(juz)
}

class SearchQuranUseCase(private val repository: QuranRepository) {
    suspend operator fun invoke(query: String, surahNumber: Int? = null): List<Ayah> =
        repository.searchAyahs(query, surahNumber)
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
    operator fun invoke(sort: BookmarkSort = BookmarkSort.NEWEST): Flow<List<Bookmark>> =
        repository.getAllBookmarks(sort)
}

class DeleteBookmarkUseCase(private val repository: QuranRepository) {
    suspend operator fun invoke(bookmark: Bookmark) = repository.deleteBookmark(bookmark.id)
}

class RestoreBookmarkUseCase(private val repository: QuranRepository) {
    suspend operator fun invoke(bookmark: Bookmark) = repository.restoreBookmark(bookmark)
}

class UpdateBookmarkNoteUseCase(private val repository: QuranRepository) {
    suspend operator fun invoke(id: Long, note: String?) = repository.updateBookmarkNote(id, note)
}

class SaveLastReadUseCase(private val repository: QuranRepository) {
    suspend operator fun invoke(
        surahNumber: Int,
        surahName: String,
        ayahNumber: Int,
        juz: Int = 1,
        page: Int = 1
    ) = repository.saveLastRead(surahNumber, surahName, ayahNumber, juz, page)
}

class GetLastReadUseCase(private val repository: QuranRepository) {
    operator fun invoke(): Flow<LastRead?> = repository.getLastRead()
}
