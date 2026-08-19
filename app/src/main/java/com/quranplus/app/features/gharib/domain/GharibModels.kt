package com.quranplus.app.features.gharib.domain

data class GharibReading(
    val id: Int,
    val categoryId: String,
    val categoryTitle: String,
    val surahNumber: Int,
    val surahName: String,
    val ayahNumber: Int,
    val pageNumber: Int,
    val wordSnippetArabic: String,
    val fullAyahArabic: String,
    val pronunciationGuide: String,
    val writtenVsSpoken: String,
    val explanation: String,
    val ruleType: GharibType
)

enum class GharibType {
    NUN_WIQAYAH,
    AYAT_SAJDAH,
    SIFIR_MUSTADIR,
    SIFIR_MUSTATIL,
    SAKTAH,
    IMALAH,
    ISYMAM,
    TASHIL,
    NAQL
}

object GharibDataRepository {
    /** Gharib data is unavailable until record-level source review is complete. */
    const val RECORD_LEVEL_REVIEW_COMPLETE = false

    val ALL_GHARIB_READINGS: List<GharibReading> = emptyList()
}
