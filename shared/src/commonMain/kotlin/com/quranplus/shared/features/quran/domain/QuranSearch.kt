package com.quranplus.shared.features.quran.domain

enum class QuranSearchField {
    ALL,
    ARABIC,
    INDONESIAN,
    ENGLISH,
    TRANSLITERATION
}

enum class QuranSearchMode {
    ALL_WORDS,
    EXACT_PHRASE
}

data class QuranSearchFilter(
    val surahNumber: Int? = null,
    val field: QuranSearchField = QuranSearchField.ALL,
    val mode: QuranSearchMode = QuranSearchMode.ALL_WORDS
)
