package com.quranplus.app.features.quran.data

import com.quranplus.app.core.database.dao.WordByWordDao
import com.quranplus.app.features.quran.domain.WordByWord
import com.quranplus.app.features.quran.domain.WordByWordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WordByWordRepositoryImpl(
    private val dao: WordByWordDao
) : WordByWordRepository {
    override fun getWordsBySurah(surahNumber: Int): Flow<List<WordByWord>> =
        dao.getWordsBySurah(surahNumber).map { words ->
            words.map { word ->
                WordByWord(
                    id = word.id,
                    surahNumber = word.surahId,
                    ayahNumber = word.ayahNumber,
                    wordIndex = word.wordIndex,
                    textArabic = word.textArabic,
                    transliteration = word.transliteration,
                    translationEn = word.translationEn.takeIf(String::isNotBlank),
                    translationId = word.translationId.takeIf(String::isNotBlank)
                )
            }
        }
}
