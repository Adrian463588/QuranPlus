package com.quranplus.app.features.quran.domain

data class WordByWord(
    val id: Long,
    val surahNumber: Int,
    val ayahNumber: Int,
    val wordIndex: Int,
    val textArabic: String,
    val transliteration: String?,
    val translationEn: String?,
    val translationId: String?,
    val sourceRevision: String,
    val sourceSha256: String
)

interface WordByWordRepository {
    fun getWordsBySurah(surahNumber: Int): kotlinx.coroutines.flow.Flow<List<WordByWord>>
}

class GetWordsBySurahUseCase(
    private val repository: WordByWordRepository
) {
    operator fun invoke(surahNumber: Int): kotlinx.coroutines.flow.Flow<List<WordByWord>> {
        require(surahNumber in 1..114) { "Nomor surah tidak valid" }
        return repository.getWordsBySurah(surahNumber)
    }
}
