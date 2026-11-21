package com.quranplus.app.core.utils

/**
 * Surah metadata mapper and lookup helper
 */
object SurahMapper {

    data class SurahRef(
        val number: Int,
        val latinName: String,
        val ayahCount: Int,
        val meaningId: String = ""
    )

    private val SURAHS = listOf(
        SurahRef(1, "Al-Fatihah", 7, "Pembukaan"),
        SurahRef(2, "Al-Baqarah", 286, "Sapi Betina"),
        SurahRef(3, "Ali 'Imran", 200, "Keluarga Imran"),
        SurahRef(4, "An-Nisa'", 176, "Perempuan"),
        SurahRef(5, "Al-Ma'idah", 120, "Hidangan"),
        SurahRef(6, "Al-An'am", 165, "Binatang Ternak"),
        SurahRef(7, "Al-A'raf", 206, "Tempat yang Tertinggi"),
        SurahRef(8, "Al-Anfal", 75, "Rampasan Perang"),
        SurahRef(9, "At-Taubah", 129, "Pengampunan"),
        SurahRef(10, "Yunus", 109, "Yunus"),
        SurahRef(11, "Hud", 123, "Hud"),
        SurahRef(12, "Yusuf", 111, "Yusuf"),
        SurahRef(13, "Ar-Ra'd", 43, "Guruh"),
        SurahRef(14, "Ibrahim", 52, "Ibrahim"),
        SurahRef(15, "Al-Hijr", 99, "Hijr"),
        SurahRef(16, "An-Nahl", 128, "Lebah"),
        SurahRef(17, "Al-Isra'", 111, "Memperjalankan di Malam Hari"),
        SurahRef(18, "Al-Kahf", 110, "Gua"),
        SurahRef(19, "Maryam", 98, "Maryam"),
        SurahRef(20, "Taha", 135, "Taha"),
        SurahRef(21, "Al-Anbiya'", 112, "Para Nabi"),
        SurahRef(22, "Al-Hajj", 78, "Haji"),
        SurahRef(23, "Al-Mu'minun", 118, "Orang-Orang Mukmin"),
        SurahRef(24, "An-Nur", 64, "Cahaya"),
        SurahRef(25, "Al-Furqan", 77, "Pembeda"),
        SurahRef(26, "Asy-Syu'ara'", 227, "Para Penyair"),
        SurahRef(27, "An-Naml", 93, "Semut"),
        SurahRef(28, "Al-Qasas", 88, "Kisah-Kisah"),
        SurahRef(29, "Al-'Ankabut", 69, "Laba-Laba"),
        SurahRef(30, "Ar-Rum", 60, "Romawi"),
        SurahRef(31, "Luqman", 34, "Luqman"),
        SurahRef(32, "As-Sajdah", 30, "Sajdah"),
        SurahRef(33, "Al-Ahzab", 73, "Golongan Yang Bersekutu"),
        SurahRef(34, "Saba'", 54, "Saba'"),
        SurahRef(35, "Fatir", 45, "Pencipta"),
        SurahRef(36, "Yasin", 83, "Yasin"),
        SurahRef(37, "As-Saffat", 182, "Barisan-Barisan"),
        SurahRef(38, "Sad", 88, "Sad"),
        SurahRef(39, "Az-Zumar", 75, "Rombongan"),
        SurahRef(40, "Ghafir", 85, "Maha Pengampun"),
        SurahRef(41, "Fussilat", 54, "Dijelaskan"),
        SurahRef(42, "Asy-Syura", 53, "Musyawarah"),
        SurahRef(43, "Az-Zukhruf", 89, "Perhiasan dari Emas"),
        SurahRef(44, "Ad-Dukhan", 59, "Kabut Asap"),
        SurahRef(45, "Al-Jasiyah", 37, "Berlutut"),
        SurahRef(46, "Al-Ahqaf", 35, "Ahqaf"),
        SurahRef(47, "Muhammad", 38, "Muhammad"),
        SurahRef(48, "Al-Fath", 29, "Kemenangan"),
        SurahRef(49, "Al-Hujurat", 18, "Kamar-Kamar"),
        SurahRef(50, "Qaf", 45, "Qaf"),
        SurahRef(51, "Az-Zariyat", 60, "Yang Menerbangkan"),
        SurahRef(52, "At-Tur", 49, "Gunung"),
        SurahRef(53, "An-Najm", 62, "Bintang"),
        SurahRef(54, "Al-Qamar", 55, "Bulan"),
        SurahRef(55, "Ar-Rahman", 78, "Yang Maha Pengasih"),
        SurahRef(56, "Al-Waqi'ah", 96, "Hari Kiamat Yang Pasti Terjadi"),
        SurahRef(57, "Al-Hadid", 29, "Besi"),
        SurahRef(58, "Al-Mujadilah", 22, "Gugatan"),
        SurahRef(59, "Al-Hasyr", 24, "Pengusiran"),
        SurahRef(60, "Al-Mumtahanah", 13, "Wanita Yang Diuji"),
        SurahRef(61, "As-Saff", 14, "Barisan"),
        SurahRef(62, "Al-Jumu'ah", 11, "Jumat"),
        SurahRef(63, "Al-Munafiqun", 11, "Orang-Orang Munafik"),
        SurahRef(64, "At-Tagabun", 18, "Pengungkapan Kesalahan"),
        SurahRef(65, "At-Talaq", 12, "Talak"),
        SurahRef(66, "At-Tahrim", 12, "Pengharaman"),
        SurahRef(67, "Al-Mulk", 30, "Kerajaan"),
        SurahRef(68, "Al-Qalam", 52, "Pena"),
        SurahRef(69, "Al-Haqqah", 52, "Hari Kiamat Yang Pasti Terjadi"),
        SurahRef(70, "Al-Ma'arij", 44, "Tempat-Tempat Naik"),
        SurahRef(71, "Nuh", 28, "Nuh"),
        SurahRef(72, "Al-Jinn", 28, "Jin"),
        SurahRef(73, "Al-Muzzammil", 20, "Orang Berkelumun"),
        SurahRef(74, "Al-Muddassir", 56, "Orang Berselimut"),
        SurahRef(75, "Al-Qiyamah", 40, "Hari Kiamat"),
        SurahRef(76, "Al-Insan", 31, "Manusia"),
        SurahRef(77, "Al-Mursalat", 50, "Malaikat Yang Diutus"),
        SurahRef(78, "An-Naba'", 40, "Berita"),
        SurahRef(79, "An-Nazi'at", 46, "Yang Mencabut Dengan Keras"),
        SurahRef(80, "'Abasa", 42, "Berwajah Masam"),
        SurahRef(81, "At-Takwir", 29, "Penggulungan"),
        SurahRef(82, "Al-Infitar", 19, "Terbelah"),
        SurahRef(83, "Al-Mutaffifin", 36, "Orang-Orang Yang Curang"),
        SurahRef(84, "Al-Insyiqaq", 25, "Terbelah"),
        SurahRef(85, "Al-Buruj", 22, "Gugusan Bintang"),
        SurahRef(86, "At-Tariq", 17, "Yang Datang Pada Malam Hari"),
        SurahRef(87, "Al-A'la", 19, "Yang Maha Tinggi"),
        SurahRef(88, "Al-Ghasyiyah", 26, "Hari Kiamat Yang Menghilangkan Kesadaran"),
        SurahRef(89, "Al-Fajr", 30, "Fajar"),
        SurahRef(90, "Al-Balad", 20, "Negeri"),
        SurahRef(91, "Asy-Syams", 15, "Matahari"),
        SurahRef(92, "Al-Lail", 21, "Malam"),
        SurahRef(93, "Ad-Duha", 11, "Duha"),
        SurahRef(94, "Asy-Syarh", 8, "Pelapangan"),
        SurahRef(95, "At-Tin", 8, "Buah Tin"),
        SurahRef(96, "Al-'Alaq", 19, "Segumpal Darah"),
        SurahRef(97, "Al-Qadr", 5, "Kemuliaan"),
        SurahRef(98, "Al-Bayyinah", 8, "Bukti Nyata"),
        SurahRef(99, "Az-Zalzalah", 8, "Guncangan"),
        SurahRef(100, "Al-'Adiyat", 11, "Kuda Perang Yang Berlari Kencang"),
        SurahRef(101, "Al-Qari'ah", 11, "Hari Kiamat"),
        SurahRef(102, "At-Takasur", 8, "Berbangga-Bangga Dalam Memperbanyak Dunia"),
        SurahRef(103, "Al-'Asr", 3, "Masa"),
        SurahRef(104, "Al-Humazah", 9, "Pengumpat"),
        SurahRef(105, "Al-Fil", 5, "Gajah"),
        SurahRef(106, "Quraisy", 4, "Orang Quraisy"),
        SurahRef(107, "Al-Ma'un", 7, "Bantuan"),
        SurahRef(108, "Al-Kausar", 3, "Nikmat Yang Banyak"),
        SurahRef(109, "Al-Kafirun", 6, "Orang-Orang Kafir"),
        SurahRef(110, "An-Nasr", 3, "Pertolongan"),
        SurahRef(111, "Al-Lahab", 5, "Gejolak Api"),
        SurahRef(112, "Al-Ikhlas", 4, "Ikhlas"),
        SurahRef(113, "Al-Falaq", 5, "Fajar"),
        SurahRef(114, "An-Nas", 6, "Manusia")
    )

    private fun stripPrefix(norm: String): String {
        return norm
            .removePrefix("asy")
            .removePrefix("ash")
            .removePrefix("al")
            .removePrefix("an")
            .removePrefix("as")
            .removePrefix("at")
            .removePrefix("az")
            .removePrefix("ar")
            .removePrefix("ad")
    }

    private fun normalize(str: String): String {
        return str.lowercase()
            .replace(Regex("""[^a-z0-9]"""), "")
    }

    private fun fuzzyNormalize(str: String): String {
        return normalize(str)
            .replace("dh", "d")
            .replace("th", "t")
            .replace("zh", "z")
            .replace("sh", "s")
            .replace("kh", "k")
            .replace("gh", "g")
            .replace("aa", "a")
            .replace("ii", "i")
            .replace("uu", "u")
            .replace("ee", "e")
            .replace("oo", "o")
    }

    fun getSurah(number: Int): SurahRef? {
        return SURAHS.getOrNull(number - 1)
    }

    fun getAllSurahs(): List<SurahRef> = SURAHS

    fun getMeaningId(number: Int): String =
        getSurah(number)?.meaningId.orEmpty()

    fun findSurah(query: String): SurahRef? {
        val norm = normalize(query)
        if (norm.isEmpty()) return null

        // Exact normalized match
        SURAHS.find { normalize(it.latinName) == norm }?.let { return it }

        // Match without prefixes
        val normWithoutPrefix = stripPrefix(norm)
        SURAHS.find {
            val surahNorm = stripPrefix(normalize(it.latinName))
            surahNorm == normWithoutPrefix
        }?.let { return it }

        // Fuzzy match
        val fuzzy = fuzzyNormalize(query)
        val fuzzyNoPrefix = stripPrefix(fuzzy)
        SURAHS.find {
            val f = fuzzyNormalize(it.latinName)
            f == fuzzy || stripPrefix(f) == fuzzyNoPrefix
        }?.let { return it }

        // Partial match
        return SURAHS.find {
            val l = normalize(it.latinName)
            l.contains(norm) || norm.contains(l) || fuzzyNormalize(it.latinName).contains(fuzzy)
        }
    }

    fun matchesSurah(
        query: String,
        surahNumber: Int,
        nameLatin: String,
        nameArabic: String = "",
        nameEnglish: String = "",
        nameMeaningId: String = ""
    ): Boolean {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return false

        // Match by number
        val num = trimmed.toIntOrNull()
        if (num != null && num == surahNumber) return true

        val canonicalLatin = getSurah(surahNumber)?.latinName.orEmpty()
        val candidateNames = listOf(nameLatin, canonicalLatin).filter { it.isNotBlank() }.distinct()

        val normQuery = normalize(trimmed)
        if (normQuery.isEmpty()) {
            return nameArabic.contains(trimmed)
        }
        val queryNoPrefix = stripPrefix(normQuery)
        val queryWordNorms = trimmed
            .split(Regex("""[\s:/.-]+"""))
            .map { stripPrefix(normalize(it)) }
            .filter { it.isNotEmpty() }

        val fuzzyQuery = fuzzyNormalize(trimmed)
        val fuzzyQueryNoPrefix = stripPrefix(fuzzyQuery)

        for (name in candidateNames) {
            val normName = normalize(name)
            if (normName == normQuery) return true
            val nameNoPrefix = stripPrefix(normName)
            if (nameNoPrefix.isNotEmpty() && nameNoPrefix == queryNoPrefix) return true
            if (normName.contains(normQuery)) return true
            if (normName.length >= 5 && normQuery.contains(normName)) return true
            if (queryNoPrefix.isNotEmpty() && nameNoPrefix.contains(queryNoPrefix)) return true
            if (nameNoPrefix.length >= 5 && queryNoPrefix.contains(nameNoPrefix)) return true
            if (nameNoPrefix.isNotEmpty() && queryWordNorms.any { it == nameNoPrefix || it == normName }) return true

            // Fuzzy transliteration matching (e.g. Ad-Dhuhaa vs Ad-Duha)
            val fuzzyName = fuzzyNormalize(name)
            if (fuzzyName == fuzzyQuery) return true
            val fuzzyNameNoPrefix = stripPrefix(fuzzyName)
            if (fuzzyNameNoPrefix.isNotEmpty() && fuzzyNameNoPrefix == fuzzyQueryNoPrefix) return true
            if (fuzzyName.contains(fuzzyQuery)) return true
            if (fuzzyName.length >= 5 && fuzzyQuery.contains(fuzzyName)) return true
            if (fuzzyQueryNoPrefix.isNotEmpty() && fuzzyNameNoPrefix.contains(fuzzyQueryNoPrefix)) return true
            if (fuzzyNameNoPrefix.length >= 5 && fuzzyQueryNoPrefix.contains(fuzzyNameNoPrefix)) return true
        }

        if (nameArabic.isNotEmpty() && nameArabic.contains(trimmed)) return true
        val resolvedMeaningId = nameMeaningId.ifBlank { getMeaningId(surahNumber) }
        if (resolvedMeaningId.isNotEmpty() && matchesIndonesianMeaning(trimmed, resolvedMeaningId)) return true
        if (nameEnglish.isNotEmpty() && nameEnglish.contains(trimmed, ignoreCase = true)) return true

        return false
    }

    private fun matchesIndonesianMeaning(query: String, meaningId: String): Boolean {
        if (meaningId.contains(query, ignoreCase = true)) return true
        val normQuery = normalizeMeaningText(query)
        if (normQuery.length < 2) return false
        return normalizeMeaningText(meaningId).contains(normQuery)
    }

    private fun normalizeMeaningText(value: String): String =
        value.lowercase()
            .replace('-', ' ')
            .replace('\'', ' ')
            .replace('’', ' ')
            .replace(Regex("""\s+"""), " ")
            .trim()

    /**
     * Parses reference string e.g. "QS. Al-Ikhlas: 1" or "QS. Hud: 49 (...)"
     */
    fun parseAyahReference(ref: String): Pair<SurahRef, Int>? {
        val clean = ref.substringBefore("(").trim()
        val match = Regex("""QS\.?\s+([^:]+):\s*(\d+)""").find(clean) ?: return null
        val surahQuery = match.groupValues[1].trim()
        val ayahNumber = match.groupValues[2].toIntOrNull()?.takeIf { it > 0 } ?: return null
        val surah = findSurah(surahQuery) ?: return null
        return Pair(surah, ayahNumber)
    }

    /**
     * Parses flexible search query into Surah + Ayah number
     * Supports: "Al-Baqarah 255", "Al-Baqarah:255", "Al-Baqarah ayat 255", "2:255", "2 255", "QS. Al-Baqarah: 255"
     */
    fun parseSearchReference(query: String): Pair<SurahRef, Int>? {
        val clean = query.trim()
        if (clean.isBlank()) return null

        parseAyahReference(clean)?.let { return it }

        // Format: "2:255" or "2 255"
        val numericMatch = Regex("""^(\d{1,3})[\s:]+(\d{1,3})$""").find(clean)
        if (numericMatch != null) {
            val surahNum = numericMatch.groupValues[1].toIntOrNull() ?: return null
            val ayahNum = numericMatch.groupValues[2].toIntOrNull() ?: return null
            val surah = getSurah(surahNum) ?: return null
            if (ayahNum in 1..surah.ayahCount) return Pair(surah, ayahNum)
        }

        // Format: "Al-Baqarah 255", "Al-Baqarah:255", "Al-Baqarah ayat 255", "Baqarah 255"
        val namedMatch = Regex("""^([a-zA-Z\s'-]+?)[\s:]+(?:ayat\s+)?(\d{1,3})$""", RegexOption.IGNORE_CASE).find(clean)
        if (namedMatch != null) {
            val surahQuery = namedMatch.groupValues[1].trim()
            val ayahNum = namedMatch.groupValues[2].toIntOrNull() ?: return null
            val surah = findSurah(surahQuery) ?: return null
            if (ayahNum in 1..surah.ayahCount) return Pair(surah, ayahNum)
        }

        return null
    }
}
