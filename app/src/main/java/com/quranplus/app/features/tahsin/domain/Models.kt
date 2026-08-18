package com.quranplus.app.features.tahsin.domain

enum class TahsinCategory(val id: String, val title: String, val description: String) {
    MAKHARIJ("MAKHARIJ", "Makharij al-Huruf", "Titik artikulasi keluarnya 17 huruf hijaiyah"),
    SIFAT("SIFAT", "Sifat al-Huruf", "Karakteristik dan cara pengucapan sifat huruf"),
    HUKUM_TAJWID("HUKUM_TAJWID", "Hukum Tajwid", "Kaidah hukum nun mati, mim mati, mad, dan idgham")
}

data class TahsinLesson(
    val id: Int,
    val category: TahsinCategory,
    val subcategory: String,
    val title: String,
    val letterArabic: String,
    val letterLatin: String,
    val description: String,
    val articulationPoint: String,
    val audioSample: String?,
    val exampleAyahText: String,
    val exampleAyahRef: String,
    val ruleType: String,
    val orderIndex: Int,
    val isCompleted: Boolean
)
