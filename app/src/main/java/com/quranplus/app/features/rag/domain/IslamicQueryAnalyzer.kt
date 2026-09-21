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
    val prefersHadith: Boolean,
    val canonicalAyahTargets: List<Pair<Int, Int>> = emptyList(),
    val coreTopicTerms: List<String> = emptyList(),
    val intentTerms: List<String> = emptyList()
)

object IslamicQueryAnalyzer {
    private val tokenPattern = Regex("[\\p{L}\\p{N}]+")

    private val stopWords = setOf(
        "apa", "apakah", "siapa", "bagaimana", "mengapa", "kenapa", "kapan", "dimana",
        "adalah", "dan", "atau", "yang", "di", "ke", "dari", "pada", "untuk", "dengan",
        "ini", "itu", "saya", "kamu", "anda", "kami", "mereka", "kita", "bisa", "tolong",
        "jelaskan", "sebutkan", "menurut", "tentang", "dalam", "apabila", "jika",
        "berikan", "nomor", "no",
        "في", "من", "ما", "هو", "هي", "عن", "على", "إلى", "هذا", "هذه"
    )

    val GENERIC_INTENT_WORDS = setOf(
        "keutamaan", "fadhilah", "fadilah", "keistimewaan", "manfaat", "pahala", "fadhilahnya",
        "baca", "membaca", "bacaan", "arti", "artinya", "makna", "penjelasan", "uraian",
        "hukum", "syarat", "rukun", "amal", "amalan", "doa", "dzikir", "zikir",
        "wirid", "hizib", "tata", "cara", "perintah", "larangan", "dalil", "ayat",
        "hadis", "hadits", "hadist", "sunnah", "sunah", "riwayat", "surah", "surat",
        "quran", "islam", "dianjurkan", "anjuran"
    )

    private val aliasGroups = listOf(
        setOf("shalat", "salat", "solat", "sembahyang"),
        setOf("hadis", "hadist", "hadits", "hadith", "sunnah"),
        setOf("fiqih", "fikih", "fiqh"),
        setOf("adab", "akhlak", "etika", "sopan", "santun"),
        setOf("ibadah", "amal", "ketaatan"),
        setOf("sejarah", "tarikh", "sirah", "kisah"),
        setOf("talak", "cerai", "rujuk", "iddah"),
        setOf("nikah", "pernikahan", "perkawinan"),
        setOf("wudhu", "wudu", "bersuci"),
        setOf("puasa", "shaum", "siyam", "ramadhan"),
        setOf("zakat", "sedekah", "infak", "infaq"),
        setOf("haji", "umrah", "manasik"),
        setOf("haram", "halal", "diharamkan", "dihalalkan", "makanan", "bangkai", "babi", "sembelihan", "maidah", "al-maidah"),
        setOf("wajib", "sunnah", "sunah", "makruh", "mubah"),
        setOf("keutamaan", "fadhilah", "fadilah", "keistimewaan", "manfaat", "pahala"),
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
                "hukum", "fiqih", "fikih", "fiqh", "halal", "haram", "diharamkan", "dihalalkan",
                "wajib", "makruh", "mubah", "talak", "cerai", "nikah", "riba", "zakat"
            ) -> IslamicQuestionDomain.HUKUM_FIQIH
            containsAny(normalized, "adab", "akhlak", "etika", "sopan", "santun", "tetangga") ->
                IslamicQuestionDomain.ADAB_AKHLAK
            containsAny(
                normalized,
                "ibadah", "shalat", "salat", "solat", "puasa", "shaum", "wudhu", "wudu",
                "tayamum", "haji", "umrah", "doa", "dzikir", "zikir", "keutamaan", "fadhilah", "fadilah"
            ) -> IslamicQuestionDomain.IBADAH
            else -> IslamicQuestionDomain.UMUM
        }

        val hasVirtueOrAmalan = containsAny(
            normalized,
            "keutamaan", "fadhilah", "fadilah", "keistimewaan", "pahala", "fadhilahnya",
            "amalan", "doa", "dzikir", "zikir", "wirid", "hizib"
        )
        val prefersHadith = containsAny(
            normalized,
            "hadis", "hadist", "hadits", "hadith", "sunnah", "sunah", "riwayat"
        ) || hasVirtueOrAmalan

        // Canonical anchor ayahs and domain keywords for foundational topics
        val canonicalTargets = mutableListOf<Pair<Int, Int>>()
        val coreTopicTerms = linkedSetOf<String>()
        val intentTerms = linkedSetOf<String>()

        tokens.forEach { token ->
            if (token in GENERIC_INTENT_WORDS) {
                intentTerms += token
            }
        }

        if (containsAny(normalized, "kursi")) {
            canonicalTargets.add(2 to 255) // QS Al-Baqarah: 255 (Ayat Kursi)
            coreTopicTerms += listOf("kursi")
            keywords += listOf("kursi", "keutamaan", "fadhilah", "agung", "surga", "setan")
        }
        if (containsAny(normalized, "talak", "cerai", "rujuk", "iddah")) {
            canonicalTargets.add(2 to 228) // QS Al-Baqarah: 228
            canonicalTargets.add(65 to 1) // QS At-Talaq: 1
            coreTopicTerms += listOf("talak", "rujuk", "iddah", "cerai")
            keywords += listOf("talak", "rujuk", "iddah", "suami", "istri")
        }
        if (containsAny(normalized, "puasa", "shaum")) {
            canonicalTargets.add(2 to 183) // QS Al-Baqarah: 183 (Kewajiban Puasa)
            val isSunnah = containsAny(normalized, "sunnah", "sunah", "anjuran", "dianjurkan", "senin", "kamis", "daud", "arafah", "asyura", "syawwal", "bidh")
            if (isSunnah) {
                coreTopicTerms += listOf("puasa", "shaum", "senin", "kamis", "daud", "arafah", "asyura", "syawwal", "bidh")
            } else {
                coreTopicTerms += listOf("puasa", "shaum", "ramadhan")
            }
            keywords += listOf("puasa", "ramadhan", "syarat", "rukun", "imsak", "pembatal")
        }
        if (containsAny(normalized, "halal", "haram", "diharamkan", "dihalalkan", "bangkai", "babi", "sembelihan")) {
            canonicalTargets.add(5 to 3) // QS Al-Ma'idah: 3 (Prinsip makanan halal/haram dalam Islam)
            coreTopicTerms += listOf("makanan", "bangkai", "babi", "sembelihan", "khamr", "darah")
            keywords += listOf("diharamkan", "makanan", "bangkai", "babi", "maidah")
        }
        if (containsAny(normalized, "haji", "umrah")) {
            canonicalTargets.add(3 to 97) // QS Ali 'Imran: 97
            coreTopicTerms += listOf("haji", "umrah")
            keywords += listOf("haji", "umrah", "ihram", "thawaf", "sai", "arafah")
        }
        if (containsAny(normalized, "wudhu", "wudu") && containsAny(normalized, "tata", "cara", "perintah", "hukum", "bersuci")) {
            canonicalTargets.add(5 to 6) // QS Al-Ma'idah: 6 (Ayat Wudhu)
            coreTopicTerms += listOf("wudhu", "wudu", "bersuci")
        }
        if (containsAny(normalized, "riba")) {
            canonicalTargets.add(2 to 275) // QS Al-Baqarah: 275 (Hukum Riba)
            coreTopicTerms += listOf("riba")
        }
        if (containsAny(normalized, "rukun") && containsAny(normalized, "islam")) {
            coreTopicTerms += listOf("rukun", "islam", "syahadat")
            keywords += listOf("rukun", "islam", "syahadat", "shalat", "zakat", "puasa", "haji")
        }
        if (containsAny(normalized, "rukun") && containsAny(normalized, "iman")) {
            coreTopicTerms += listOf("rukun", "iman")
            keywords += listOf("rukun", "iman", "allah", "malaikat", "kitab", "rasul", "kiamat", "takdir")
        }

        // Fallback for general substantive topics not mapped above
        if (coreTopicTerms.isEmpty()) {
            tokens.filter { it !in GENERIC_INTENT_WORDS && it !in stopWords && it.length >= 3 }
                .forEach { coreTopicTerms += it }
        }

        val prefersQuran = containsAny(normalized, "quran", "qur'an", "surah", "surat", "ayat") ||
            canonicalTargets.isNotEmpty()

        val boundedKeywords = keywords.take(MAX_KEYWORDS)
        val expandedQuery = (listOf(query.trim()) + boundedKeywords).filter(String::isNotBlank)

        return IslamicQueryPlan(
            domain = domain,
            keywords = boundedKeywords,
            semanticQuery = expandedQuery.distinct().joinToString(" "),
            prefersQuran = prefersQuran,
            prefersHadith = prefersHadith,
            canonicalAyahTargets = canonicalTargets.distinct(),
            coreTopicTerms = coreTopicTerms.toList(),
            intentTerms = intentTerms.toList()
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
