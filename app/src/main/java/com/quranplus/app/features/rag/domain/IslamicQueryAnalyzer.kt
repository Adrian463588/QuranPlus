package com.quranplus.app.features.rag.domain

/**
 * Turns an Indonesian religious question into a bounded retrieval plan.
 *
 * The aliases are query expansion only. They are never used as answer content
 * and must resolve to a real Quran or Hadith row before reaching the LLM.
 */
enum class IslamicQuestionDomain {
    HUKUM_FIQIH,
    ADAB_AKHLAK,
    IBADAH,
    SEJARAH_TARIKH,
    UMUM
}

data class IslamicQueryPlan(
    val domain: IslamicQuestionDomain,
    val keywords: List<String>,
    val semanticQuery: String,
    val prefersQuran: Boolean,
    val prefersHadith: Boolean
)

object IslamicQueryAnalyzer {
    private val tokenPattern = Regex("[\\p{L}\\p{N}]+")

    private val stopWords = setOf(
        "apa", "apakah", "siapa", "bagaimana", "mengapa", "kenapa", "kapan", "dimana",
        "adalah", "dan", "atau", "yang", "di", "ke", "dari", "pada", "untuk", "dengan",
        "ini", "itu", "saya", "kamu", "anda", "kami", "mereka", "kita", "bisa", "tolong",
        "jelaskan", "sebutkan", "hukum", "menurut", "tentang", "dalam", "apabila", "jika",
        "berikan", "islam", "agama", "allah", "syarat", "baca", "membaca", "keutamaan",
        "fadilah", "ayat", "ayatnya", "surah", "surat", "nomor", "no",
        "في", "من", "ما", "هو", "هي", "عن", "على", "إلى", "هذا", "هذه", "الإسلام", "الاسلام"
    )

    private val aliasGroups = listOf(
        setOf("shalat", "salat", "solat", "sembahyang"),
        setOf("hadis", "hadist", "hadits", "hadith", "sunnah"),
        setOf("fiqih", "fikih", "fiqh"),
        setOf("adab", "akhlak", "etika", "sopan", "santun"),
        setOf("ibadah", "amal", "ketaatan"),
        setOf("sejarah", "tarikh", "sirah", "kisah"),
        setOf("talak", "cerai", "rujuk"),
        setOf("nikah", "pernikahan", "perkawinan"),
        setOf("wudhu", "wudu", "bersuci"),
        setOf("puasa", "shaum", "siyam"),
        setOf("zakat", "sedekah", "infak", "infaq"),
        setOf("haji", "umrah"),
        setOf("haram", "halal", "wajib", "sunnah", "sunah", "makruh", "mubah"),
        setOf("hijrah", "makkah", "mekah", "madinah"),
        setOf("nabi", "rasul", "sahabat", "khulafaur")
    )

    fun analyze(query: String): IslamicQueryPlan {
        val normalized = normalize(query)
        val tokens = tokenPattern.findAll(normalized)
            .map { it.value }
            .filter { token -> (token.length >= 3 || token.all(Char::isDigit)) && token !in stopWords }
            .toList()

        val keywords = linkedSetOf<String>()
        tokens.forEach { token ->
            keywords += token
            aliasGroups.firstOrNull { token in it }?.let { aliases ->
                keywords += aliases
            }
        }
        if (keywords.isEmpty()) {
            tokenPattern.findAll(normalized)
                .map { it.value }
                .filter { it.length >= 2 }
                .forEach(keywords::add)
        }

        val domain = when {
            containsAny(normalized, "sejarah", "tarikh", "sirah", "hijrah", "nabi", "rasul") ->
                IslamicQuestionDomain.SEJARAH_TARIKH
            containsAny(
                normalized,
                "hukum", "fiqih", "fikih", "fiqh", "halal", "haram", "wajib", "makruh",
                "mubah", "talak", "cerai", "nikah", "riba", "zakat"
            ) -> IslamicQuestionDomain.HUKUM_FIQIH
            containsAny(normalized, "adab", "akhlak", "etika", "sopan", "santun", "tetangga") ->
                IslamicQuestionDomain.ADAB_AKHLAK
            containsAny(
                normalized,
                "ibadah", "shalat", "salat", "solat", "puasa", "shaum", "wudhu", "wudu",
                "tayamum", "haji", "umrah", "doa", "dzikir", "zikir"
            ) -> IslamicQuestionDomain.IBADAH
            else -> IslamicQuestionDomain.UMUM
        }

        val prefersQuran = containsAny(normalized, "quran", "qur'an", "ayat", "surah", "surat")
        val prefersHadith = containsAny(
            normalized,
            "hadis", "hadist", "hadits", "hadith", "sunnah", "sunah", "riwayat"
        )
        val boundedKeywords = keywords.take(MAX_KEYWORDS)
        val expandedQuery = (listOf(query.trim()) + boundedKeywords).filter(String::isNotBlank)

        return IslamicQueryPlan(
            domain = domain,
            keywords = boundedKeywords,
            semanticQuery = expandedQuery.distinct().joinToString(" "),
            prefersQuran = prefersQuran,
            prefersHadith = prefersHadith
        )
    }

    private fun containsAny(value: String, vararg terms: String): Boolean {
        val tokens = value.splitToTokens()
        return terms.any { term -> normalize(term) in tokens }
    }

    private fun normalize(value: String): String =
        value.lowercase()
            .replace('’', '\'')
            .replace("qur'an", "quran")

    private fun String.splitToTokens(): Set<String> =
        tokenPattern.findAll(this).map { it.value }.toSet()

    private const val MAX_KEYWORDS = 12
}
