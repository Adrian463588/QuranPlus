package com.quranplus.app.features.quran.domain

data class Surah(
    val number: Int,
    val nameArabic: String,
    val nameLatin: String,
    val nameEnglish: String,
    val revelationType: String,
    val ayahCount: Int
)

data class Ayah(
    val id: Long,
    val surahNumber: Int,
    val ayahNumber: Int,
    val textArabic: String,
    val transliteration: String,
    val translationId: String,
    val translationEn: String,
    val juz: Int = 1,
    val page: Int = 1,
    val tajwidTags: String? = null,
    val isBookmarked: Boolean = false
)

data class Bookmark(
    val id: Long,
    val surahNumber: Int,
    val surahName: String,
    val ayahNumber: Int,
    val ayahTextArabic: String,
    val ayahTranslation: String,
    val note: String?,
    val timestamp: Long
)

data class LastRead(
    val surahNumber: Int,
    val surahName: String,
    val ayahNumber: Int,
    val timestamp: Long
)
