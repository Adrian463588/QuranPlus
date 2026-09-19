package com.quranplus.app.features.quran.domain

typealias Surah = com.quranplus.shared.features.quran.domain.Surah
typealias Ayah = com.quranplus.shared.features.quran.domain.Ayah
typealias Bookmark = com.quranplus.shared.features.quran.domain.Bookmark
typealias LastRead = com.quranplus.shared.features.quran.domain.LastRead
typealias QuranSearchField = com.quranplus.shared.features.quran.domain.QuranSearchField
typealias QuranSearchMode = com.quranplus.shared.features.quran.domain.QuranSearchMode
typealias QuranSearchFilter = com.quranplus.shared.features.quran.domain.QuranSearchFilter
typealias Tafsir = com.quranplus.shared.features.quran.domain.Tafsir


data class JuzInfo(
    val number: Int,
    val nameArabic: String,
    val startSurahNumber: Int,
    val startSurahName: String,
    val startAyahNumber: Int,
    val endSurahNumber: Int,
    val endSurahName: String,
    val endAyahNumber: Int,
    val totalVerses: Int
) {
    val startDescription: String
        get() = "MULAI DI: ${startSurahName.uppercase()} AYAT $startAyahNumber"
}

object JuzCatalog {
    val ALL_JUZ: List<JuzInfo> = listOf(
        JuzInfo(1, "آلم", 1, "Al-Fatihah", 1, 2, "Al-Baqarah", 141, 148),
        JuzInfo(2, "سَيَقُولُ", 2, "Al-Baqarah", 142, 2, "Al-Baqarah", 252, 111),
        JuzInfo(3, "تِلْكَ ٱلرُّسُلُ", 2, "Al-Baqarah", 253, 3, "Ali 'Imran", 92, 126),
        JuzInfo(4, "لَن تَنَالُوا۟", 3, "Ali 'Imran", 93, 4, "An-Nisa'", 23, 131),
        JuzInfo(5, "وَٱلْمُحْصَنَٰتُ", 4, "An-Nisa'", 24, 4, "An-Nisa'", 147, 124),
        JuzInfo(6, "لَا يُحِبُّ ٱللَّهُ", 4, "An-Nisa'", 148, 5, "Al-Ma'idah", 81, 110),
        JuzInfo(7, "وَإِذَا سَمِعُوا۟", 5, "Al-Ma'idah", 82, 6, "Al-An'am", 110, 149),
        JuzInfo(8, "وَلَوْ أَنَّنَا", 6, "Al-An'am", 111, 7, "Al-A'raf", 87, 142),
        JuzInfo(9, "قَالَ ٱلْمَلَأُ", 7, "Al-A'raf", 88, 8, "Al-Anfal", 40, 159),
        JuzInfo(10, "وَٱعْلَمُوٓا۟", 8, "Al-Anfal", 41, 9, "At-Taubah", 92, 127),
        JuzInfo(11, "يَعْتَذِرُونَ", 9, "At-Taubah", 93, 11, "Hud", 5, 151),
        JuzInfo(12, "وَمَا مِن دَآبَّةٍ", 11, "Hud", 6, 12, "Yusuf", 52, 170),
        JuzInfo(13, "وَمَا أُبَرِّئُ", 12, "Yusuf", 53, 14, "Ibrahim", 52, 155),
        JuzInfo(14, "رُّبَمَا", 15, "Al-Hijr", 1, 16, "An-Nahl", 128, 227),
        JuzInfo(15, "سُبْحَٰنَ ٱلَّذِى", 17, "Al-Isra'", 1, 18, "Al-Kahf", 74, 185),
        JuzInfo(16, "قَالَ أَلَمْ", 18, "Al-Kahf", 75, 20, "Ta-Ha", 135, 269),
        JuzInfo(17, "ٱقْتَرَبَ", 21, "Al-Anbiya'", 1, 22, "Al-Hajj", 78, 190),
        JuzInfo(18, "قَدْ أَفْلَحَ", 23, "Al-Mu'minun", 1, 25, "Al-Furqan", 20, 202),
        JuzInfo(19, "وَقَالَ ٱلَّذِينَ", 25, "Al-Furqan", 21, 27, "An-Naml", 55, 339),
        JuzInfo(20, "أَمَّنْ خَلَقَ", 27, "An-Naml", 56, 29, "Al-'Ankabut", 45, 171),
        JuzInfo(21, "اتْلُ مَا أُوحِيَ", 29, "Al-'Ankabut", 46, 33, "Al-Ahzab", 30, 178),
        JuzInfo(22, "وَمَن يَقْنُتْ", 33, "Al-Ahzab", 31, 36, "Ya-Sin", 27, 169),
        JuzInfo(23, "وَمَآ أَنزَلْنَا", 36, "Ya-Sin", 28, 39, "Az-Zumar", 31, 357),
        JuzInfo(24, "فَمَنْ أَظْلَمُ", 39, "Az-Zumar", 32, 41, "Fussilat", 46, 175),
        JuzInfo(25, "إِلَيْهِ يُرَدُّ", 41, "Fussilat", 47, 45, "Al-Jasiyah", 37, 246),
        JuzInfo(26, "حم", 46, "Al-Ahqaf", 1, 51, "Az-Zariyat", 30, 195),
        JuzInfo(27, "قَالَ فَمَا خَطْبُكُمْ", 51, "Az-Zariyat", 31, 57, "Al-Hadid", 29, 399),
        JuzInfo(28, "قَدْ سَمِعَ", 58, "Al-Mujadilah", 1, 66, "At-Tahrim", 12, 137),
        JuzInfo(29, "تَبَٰرَكَ ٱلَّذِى", 67, "Al-Mulk", 1, 77, "Al-Mursalat", 50, 431),
        JuzInfo(30, "عَمَّ يَتَسَآءَلُونَ", 78, "An-Naba'", 1, 114, "An-Nas", 6, 564)
    )
}
