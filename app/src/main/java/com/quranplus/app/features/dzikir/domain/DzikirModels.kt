package com.quranplus.app.features.dzikir.domain

enum class DzikirCategory(
    val id: String,
    val title: String,
    val subtitle: String,
    val orderIndex: Int
) {
    PAGI(
        id = "PAGI",
        title = "Dzikir Pagi",
        subtitle = "Amalan pembuka hari pengetuk pintu rezeki dan perlindungan",
        orderIndex = 1
    ),
    PETANG(
        id = "PETANG",
        title = "Dzikir Petang",
        subtitle = "Amalan sore hari penjaga dari segala bahaya dan gangguan",
        orderIndex = 2
    ),
    SHALAT(
        id = "SHALAT",
        title = "Setelah Shalat Fardhu",
        subtitle = "Wirid dan doa ma'tsur setelah shalat lima waktu",
        orderIndex = 3
    ),
    AL_MATSURAT(
        id = "AL_MATSURAT",
        title = "Al-Ma'tsurat Sughra",
        subtitle = "Kompilasi wirid harian susunan Imam Hasan Al-Banna",
        orderIndex = 4
    ),
    RATIB_AL_HADDAD(
        id = "RATIB_AL_HADDAD",
        title = "Ratib Al-Haddad",
        subtitle = "Rangkaian wirid agung Al-Habib Abdullah bin Alawi Al-Haddad",
        orderIndex = 5
    ),
    WIRDUL_LATIF(
        id = "WIRDUL_LATIF",
        title = "Wirdul Latif",
        subtitle = "Untaian dzikir pagi dan sore Al-Habib Abdullah bin Alawi Al-Haddad",
        orderIndex = 6
    ),
    HIZIB_BAHR(
        id = "HIZIB_BAHR",
        title = "Hizib Bahr",
        subtitle = "Hizib keselamatan dan pertolongan Syaikh Abul Hasan Asy-Syadzili",
        orderIndex = 7
    ),
    HIZIB_NASHR(
        id = "HIZIB_NASHR",
        title = "Hizib Nashr Asy-Syadzili",
        subtitle = "Hizib kemenangan dan perisai dari musuh Syaikh Abul Hasan Asy-Syadzili",
        orderIndex = 8
    ),
    HIZIB_NASHR_HADDAD(
        id = "HIZIB_NASHR_HADDAD",
        title = "Hizib Nashr Al-Haddad",
        subtitle = "Hizib pembuka kemenangan Al-Habib Abdullah bin Alawi Al-Haddad",
        orderIndex = 9
    ),
    HIZIB_NAWAWI(
        id = "HIZIB_NAWAWI",
        title = "Hizib Nawawi",
        subtitle = "Untaian doa benteng diri, keluarga, dan harta Imam An-Nawawi",
        orderIndex = 10
    )
}

data class DzikirItem(
    val id: String,
    val category: DzikirCategory,
    val orderNumber: Int,
    val title: String,
    val arabicText: String,
    val tajwidTags: String? = null,
    val transliteration: String,
    val translationId: String,
    val translationEn: String,
    val repeatCount: Int,
    val sourceNote: String,
    val fadhilah: String? = null
)

data class DzikirUiSettings(
    val arabicFontSize: Float = 26f,
    val showTajwid: Boolean = true,
    val showTransliteration: Boolean = true,
    val showTranslation: Boolean = true,
    val translationLanguage: TranslationLanguage = TranslationLanguage.INDONESIAN
)

enum class TranslationLanguage {
    INDONESIAN,
    ENGLISH,
    BOTH
}
