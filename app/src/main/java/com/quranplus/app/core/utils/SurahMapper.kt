package com.quranplus.app.core.utils

/**
 * Surah metadata mapper and lookup helper
 */
object SurahMapper {

    data class SurahRef(
        val number: Int,
        val latinName: String,
        val ayahCount: Int
    )

    private val SURAHS = listOf(
        SurahRef(1, "Al-Fatihah", 7),
        SurahRef(2, "Al-Baqarah", 286),
        SurahRef(3, "Ali 'Imran", 200),
        SurahRef(4, "An-Nisa'", 176),
        SurahRef(5, "Al-Ma'idah", 120),
        SurahRef(6, "Al-An'am", 165),
        SurahRef(7, "Al-A'raf", 206),
        SurahRef(8, "Al-Anfal", 75),
        SurahRef(9, "At-Taubah", 129),
        SurahRef(10, "Yunus", 109),
        SurahRef(11, "Hud", 123),
        SurahRef(12, "Yusuf", 111),
        SurahRef(13, "Ar-Ra'd", 43),
        SurahRef(14, "Ibrahim", 52),
        SurahRef(15, "Al-Hijr", 99),
        SurahRef(16, "An-Nahl", 128),
        SurahRef(17, "Al-Isra'", 111),
        SurahRef(18, "Al-Kahf", 110),
        SurahRef(19, "Maryam", 98),
        SurahRef(20, "Taha", 135),
        SurahRef(21, "Al-Anbiya'", 112),
        SurahRef(22, "Al-Hajj", 78),
        SurahRef(23, "Al-Mu'minun", 118),
        SurahRef(24, "An-Nur", 64),
        SurahRef(25, "Al-Furqan", 77),
        SurahRef(26, "Asy-Syu'ara'", 227),
        SurahRef(27, "An-Naml", 93),
        SurahRef(28, "Al-Qasas", 88),
        SurahRef(29, "Al-'Ankabut", 69),
        SurahRef(30, "Ar-Rum", 60),
        SurahRef(31, "Luqman", 34),
        SurahRef(32, "As-Sajdah", 30),
        SurahRef(33, "Al-Ahzab", 73),
        SurahRef(34, "Saba'", 54),
        SurahRef(35, "Fatir", 45),
        SurahRef(36, "Yasin", 83),
        SurahRef(37, "As-Saffat", 182),
        SurahRef(38, "Sad", 88),
        SurahRef(39, "Az-Zumar", 75),
        SurahRef(40, "Ghafir", 85),
        SurahRef(41, "Fussilat", 54),
        SurahRef(42, "Asy-Syura", 53),
        SurahRef(43, "Az-Zukhruf", 89),
        SurahRef(44, "Ad-Dukhan", 59),
        SurahRef(45, "Al-Jasiyah", 37),
        SurahRef(46, "Al-Ahqaf", 35),
        SurahRef(47, "Muhammad", 38),
        SurahRef(48, "Al-Fath", 29),
        SurahRef(49, "Al-Hujurat", 18),
        SurahRef(50, "Qaf", 45),
        SurahRef(51, "Az-Zariyat", 60),
        SurahRef(52, "At-Tur", 49),
        SurahRef(53, "An-Najm", 62),
        SurahRef(54, "Al-Qamar", 55),
        SurahRef(55, "Ar-Rahman", 78),
        SurahRef(56, "Al-Waqi'ah", 96),
        SurahRef(57, "Al-Hadid", 29),
        SurahRef(58, "Al-Mujadilah", 22),
        SurahRef(59, "Al-Hasyr", 24),
        SurahRef(60, "Al-Mumtahanah", 13),
        SurahRef(61, "As-Saff", 14),
        SurahRef(62, "Al-Jumu'ah", 11),
        SurahRef(63, "Al-Munafiqun", 11),
        SurahRef(64, "At-Tagabun", 18),
        SurahRef(65, "At-Talaq", 12),
        SurahRef(66, "At-Tahrim", 12),
        SurahRef(67, "Al-Mulk", 30),
        SurahRef(68, "Al-Qalam", 52),
        SurahRef(69, "Al-Haqqah", 52),
        SurahRef(70, "Al-Ma'arij", 44),
        SurahRef(71, "Nuh", 28),
        SurahRef(72, "Al-Jinn", 28),
        SurahRef(73, "Al-Muzzammil", 20),
        SurahRef(74, "Al-Muddassir", 56),
        SurahRef(75, "Al-Qiyamah", 40),
        SurahRef(76, "Al-Insan", 31),
        SurahRef(77, "Al-Mursalat", 50),
        SurahRef(78, "An-Naba'", 40),
        SurahRef(79, "An-Nazi'at", 46),
        SurahRef(80, "'Abasa", 42),
        SurahRef(81, "At-Takwir", 29),
        SurahRef(82, "Al-Infitar", 19),
        SurahRef(83, "Al-Mutaffifin", 36),
        SurahRef(84, "Al-Insyiqaq", 25),
        SurahRef(85, "Al-Buruj", 22),
        SurahRef(86, "At-Tariq", 17),
        SurahRef(87, "Al-A'la", 19),
        SurahRef(88, "Al-Ghasyiyah", 26),
        SurahRef(89, "Al-Fajr", 30),
        SurahRef(90, "Al-Balad", 20),
        SurahRef(91, "Asy-Syams", 15),
        SurahRef(92, "Al-Lail", 21),
        SurahRef(93, "Ad-Duha", 11),
        SurahRef(94, "Asy-Syarh", 8),
        SurahRef(95, "At-Tin", 8),
        SurahRef(96, "Al-'Alaq", 19),
        SurahRef(97, "Al-Qadr", 5),
        SurahRef(98, "Al-Bayyinah", 8),
        SurahRef(99, "Az-Zalzalah", 8),
        SurahRef(100, "Al-'Adiyat", 11),
        SurahRef(101, "Al-Qari'ah", 11),
        SurahRef(102, "At-Takasur", 8),
        SurahRef(103, "Al-'Asr", 3),
        SurahRef(104, "Al-Humazah", 9),
        SurahRef(105, "Al-Fil", 5),
        SurahRef(106, "Quraisy", 4),
        SurahRef(107, "Al-Ma'un", 7),
        SurahRef(108, "Al-Kausar", 3),
        SurahRef(109, "Al-Kafirun", 6),
        SurahRef(110, "An-Nasr", 3),
        SurahRef(111, "Al-Lahab", 5),
        SurahRef(112, "Al-Ikhlas", 4),
        SurahRef(113, "Al-Falaq", 5),
        SurahRef(114, "An-Nas", 6)
    )

    private fun normalize(str: String): String {
        return str.lowercase()
            .replace(Regex("""[^a-z0-9]"""), "")
    }

    fun getSurah(number: Int): SurahRef? {
        return SURAHS.getOrNull(number - 1)
    }

    fun findSurah(query: String): SurahRef? {
        val norm = normalize(query)
        if (norm.isEmpty()) return null

        // Exact normalized match
        SURAHS.find { normalize(it.latinName) == norm }?.let { return it }

        // Match without "al" or "an" prefix
        val normWithoutPrefix = norm.removePrefix("al").removePrefix("an").removePrefix("as").removePrefix("at").removePrefix("az").removePrefix("ar")
        SURAHS.find {
            val surahNorm = normalize(it.latinName).removePrefix("al").removePrefix("an").removePrefix("as").removePrefix("at").removePrefix("az").removePrefix("ar")
            surahNorm == normWithoutPrefix
        }?.let { return it }

        // Partial match
        return SURAHS.find { normalize(it.latinName).contains(norm) || norm.contains(normalize(it.latinName)) }
    }

    /**
     * Parses reference string e.g. "QS. Al-Ikhlas: 1" or "QS. Hud: 49 (...)"
     */
    fun parseAyahReference(ref: String): Pair<SurahRef, Int>? {
        val clean = ref.substringBefore("(").trim()
        val match = Regex("""QS\.?\s+([^:]+):\s*(\d+)""").find(clean) ?: return null
        val surahQuery = match.groupValues[1].trim()
        val ayahNumber = match.groupValues[2].toIntOrNull() ?: 1
        val surah = findSurah(surahQuery) ?: return null
        return Pair(surah, ayahNumber)
    }
}
