package com.quranplus.app.core.utils

import androidx.compose.ui.graphics.Color
import com.quranplus.app.core.ui.theme.QuranColors

/**
 * Waqaf Parser and Guide Helper
 * Manages Waqaf markers, End-of-Ayah formatting, and interactive Waqaf explanations.
 */
object WaqafParser {

    // Unicode symbols for Waqaf markers
    const val WAQAF_LA_SYM       = "ۘ" // U+06D8 (لا)
    const val WAQAF_JAIZ_SYM     = "ۚ" // U+06DA (ج)
    const val WAQAF_WASHLA_SYM   = "ۖ" // U+06D6 (صلى)
    const val WAQAF_AWLA_SYM     = "ۗ" // U+06D7 (قلى)
    const val WAQAF_MUANAQAH_SYM = "ۛ" // U+06DB (∴)
    const val WAQAF_SAKTAH_SYM   = "ۜ" // U+06DC (س / سكتة)
    const val WAQAF_LAZIM_SYM    = "ۙ" // U+06D9 (م)
    const val AYAH_END_SYM       = "۝" // U+06DD End of Ayah

    data class WaqafRule(
        val symbol: String,
        val arabicName: String,
        val latinName: String,
        val meaning: String,
        val recommendation: String,
        val actionCategory: ActionCategory,
        val badgeColor: Color,
        val detailedRule: String,
        val exampleAyah: String,
        val exampleRef: String
    )

    enum class ActionCategory(val label: String, val badgeColor: Color) {
        FORBIDDEN("Dilarang Berhenti", QuranColors.BadgeWaqafStop),
        MANDATORY("Wajib / Diutamakan Berhenti", QuranColors.BadgeWaqafContinue),
        OPTIONAL("Boleh Berhenti / Lanjut", QuranColors.BadgeWaqafOptional)
    }

    val ALL_WAQAF_RULES: List<WaqafRule> = listOf(
        WaqafRule(
            symbol = "لا",
            arabicName = "لا وقف فيه",
            latinName = "La Waqfa Fih (لا)",
            meaning = "Dilarang berhenti di tengah kalimat",
            recommendation = "Lebih baik diteruskan, kecuali di akhir ayat disunnahkan waqaf",
            actionCategory = ActionCategory.FORBIDDEN,
            badgeColor = QuranColors.BadgeWaqafStop,
            detailedRule = "Dilarang berhenti jika tanda ini berada di tengah ayat karena dapat merusak makna. Namun jika berada di akhir ayat, pembaca tetap boleh berhenti mengikuti sunnah Rasulullah SAW.",
            exampleAyah = "الَّذِينَ تَتَوَفَّاهُمُ الْمَلَائِكَةُ طَيِّبِينَ ۙ يَقُولُونَ سَلَامٌ عَلَيْكُمُ",
            exampleRef = "QS. An-Nahl: 32"
        ),
        WaqafRule(
            symbol = "م",
            arabicName = "الوقف اللازم",
            latinName = "Waqaf Lazim (م)",
            meaning = "Diharuskan / Wajib Berhenti",
            recommendation = "Wajib berhenti demi kesempurnaan makna ayat",
            actionCategory = ActionCategory.MANDATORY,
            badgeColor = QuranColors.BadgeWaqafContinue,
            detailedRule = "Pembaca sangat dianjurkan berhenti sempurna. Jika diteruskan (wasal), makna ayat dapat menjadi rancu atau bertentangan dengan makna sebenarnya.",
            exampleAyah = "إِنَّمَا يَسْتَجِيبُ الَّذِينَ يَسْمَعُونَ ۘ وَالْمَوْتَىٰ يَبْعَثُهُمُ اللَّهُ",
            exampleRef = "QS. Al-An'am: 36"
        ),
        WaqafRule(
            symbol = "قلى",
            arabicName = "الوقف أولى",
            latinName = "Al-Waqfu Ula (قلى)",
            meaning = "Lebih baik / diutamakan berhenti",
            recommendation = "Diutamakan berhenti (waqaf), namun boleh diteruskan (wasal)",
            actionCategory = ActionCategory.MANDATORY,
            badgeColor = QuranColors.BadgeWaqafContinue,
            detailedRule = "Berhenti pada tanda ini lebih utama daripada melanjutkan bacaan, walaupun melanjutkan juga diperbolehkan tanpa mengubah arti secara fatal.",
            exampleAyah = "قُلْ رَبِّي أَعْلَمُ بِعِدَّتِهِمْ مَا يَعْلَمُهُمْ إِلَّا قَلِيلٌ ۗ فَلَا تُمَارِ فِيهِمْ",
            exampleRef = "QS. Al-Kahf: 22"
        ),
        WaqafRule(
            symbol = "صلى",
            arabicName = "الوصل أولى",
            latinName = "Al-Washlu Ula (صلى)",
            meaning = "Lebih baik diteruskan (wasal)",
            recommendation = "Melanjutkan bacaan lebih utama, namun boleh berhenti jika nafas tidak cukup",
            actionCategory = ActionCategory.OPTIONAL,
            badgeColor = QuranColors.BadgeWaqafOptional,
            detailedRule = "Melanjutkan bacaan (wasal) lebih utama dan lebih sempurna secara gramatika kalimat, tetapi diperbolehkan berhenti jika kehabisan nafas tanpa perlu mengulang.",
            exampleAyah = "وَإِنْ يَمْسَسْكَ اللَّهُ بِضُرٍّ فَلَا كَاشِفَ لَهُ إِلَّا هُوَ ۖ وَإِنْ يَمْسَسْكَ بِخَيْرٍ",
            exampleRef = "QS. Al-An'am: 17"
        ),
        WaqafRule(
            symbol = "ج",
            arabicName = "الوقف الجائز",
            latinName = "Waqaf Jaiz (ج)",
            meaning = "Boleh berhenti atau meneruskan",
            recommendation = "Boleh berhenti dan boleh lanjut dengan kedudukan sama kuat",
            actionCategory = ActionCategory.OPTIONAL,
            badgeColor = QuranColors.BadgeWaqafOptional,
            detailedRule = "Pembaca bebas memilih untuk berhenti atau melanjutkan bacaan karena kalimat sudah cukup sempurna maknanya.",
            exampleAyah = "نَحْنُ نَقُصُّ عَلَيْكَ نَبَأَهُمْ بِالْحَقِّ ۚ إِنَّهُمْ فِتْيَةٌ آمَنُوا بِرَبِّهِمْ",
            exampleRef = "QS. Al-Kahf: 13"
        ),
        WaqafRule(
            symbol = "∴ ∴",
            arabicName = "وقف المعانقة",
            latinName = "Waqaf Mu'anaqah / Muraqabah (∴ ∴)",
            meaning = "Berhenti pada salah satu tanda",
            recommendation = "Wajib berhenti di salah satu tanda titik tiga, dilarang berhenti di kedua-duanya",
            actionCategory = ActionCategory.OPTIONAL,
            badgeColor = QuranColors.BadgeWaqafOptional,
            detailedRule = "Terdapat sepasang tanda titik tiga berdekatan. Pembaca harus berhenti pada salah satu titik tiga, dan tidak boleh berhenti pada kedua-duanya atau melewati kedua-duanya tanpa berhenti.",
            exampleAyah = "ذَٰلِكَ الْكِتَابُ لَا رَيْبَ ۛ فِيهِ ۛ هُدًى لِلْمُتَّقِينَ",
            exampleRef = "QS. Al-Baqarah: 2"
        ),
        WaqafRule(
            symbol = "س",
            arabicName = "السكتة",
            latinName = "Saktah (س)",
            meaning = "Berhenti sejenak tanpa bernafas",
            recommendation = "Berhenti selama ±2 harakat tanpa mengambil nafas baru",
            actionCategory = ActionCategory.MANDATORY,
            badgeColor = QuranColors.BadgeWaqafContinue,
            detailedRule = "Menahan suara sejenak sekitar 1 alif (2 harakat) tanpa menarik nafas baru, lalu langsung melanjutkan ke kata berikutnya.",
            exampleAyah = "وَلَمْ يَجْعَلْ لَهُ عِوَجًا ۜ قَيِّمًا لِيُنْذِرَ بَأْسًا شَدِيدًا",
            exampleRef = "QS. Al-Kahf: 1-2"
        )
    )

    /**
     * Converts a Western number (e.g. 1, 25) to Eastern Arabic Numerals (١, ٢٥).
     */
    fun toArabicDigits(number: Int): String {
        val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        return number.toString().map { char ->
            if (char in '0'..'9') arabicDigits[char - '0'] else char
        }.joinToString("")
    }

    /**
     * Formats end-of-ayah marker symbol with embedded Arabic digits.
     */
    fun formatAyahEndMarker(ayahNumber: Int): String {
        return " ۝${toArabicDigits(ayahNumber)} "
    }

    /**
     * Finds matching Waqaf rule by symbol character.
     */
    fun findRuleBySymbol(char: Char): WaqafRule? {
        return when (char) {
            'ۘ' -> ALL_WAQAF_RULES.find { it.symbol == "م" }
            'ۚ' -> ALL_WAQAF_RULES.find { it.symbol == "ج" }
            'ۖ' -> ALL_WAQAF_RULES.find { it.symbol == "صلى" }
            'ۗ' -> ALL_WAQAF_RULES.find { it.symbol == "قلى" }
            'ۛ' -> ALL_WAQAF_RULES.find { it.symbol.contains("∴") }
            'ۜ' -> ALL_WAQAF_RULES.find { it.symbol == "س" }
            'ۙ' -> ALL_WAQAF_RULES.find { it.symbol == "لا" }
            else -> null
        }
    }
}
