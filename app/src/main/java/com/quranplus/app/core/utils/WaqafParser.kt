package com.quranplus.app.core.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import com.quranplus.app.core.ui.theme.QuranColors

/**
 * Waqaf marker formatting and provenance gate.
 *
 * Marker semantics are mapped to the Uthmani glyphs stored in the bundled
 * Quran corpus. The mapping follows the reviewed Sprint 2 catalog; unknown
 * glyphs remain unannotated rather than being guessed.
 */
object WaqafParser {

    const val SOURCE_CATALOG_VERIFIED = true
    const val WAQAF_ANNOTATION = "quranplus_waqaf"
    const val WAQAF_PAIR_ANNOTATION = "quranplus_waqaf_pair"
    const val AYAH_END_ANNOTATION = "quranplus_ayah_end"

    const val WAQAF_LA_SYM = "ۙ"
    const val WAQAF_JAIZ_SYM = "ۚ"
    const val WAQAF_WASHLA_SYM = "ۖ"
    const val WAQAF_AWLA_SYM = "ۗ"
    const val WAQAF_MUANAQAH_SYM = "ۛ"
    const val WAQAF_SAKTAH_SYM = "ۜ"
    const val WAQAF_LAZIM_SYM = "ۘ"
    const val AYAH_END_SYM = "۝"
    val WAQAF_MARKER_SYMBOLS: Set<Char> = setOf(
        WAQAF_LA_SYM.single(),
        WAQAF_JAIZ_SYM.single(),
        WAQAF_WASHLA_SYM.single(),
        WAQAF_AWLA_SYM.single(),
        WAQAF_MUANAQAH_SYM.single(),
        WAQAF_SAKTAH_SYM.single(),
        WAQAF_LAZIM_SYM.single()
    )

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
        val exampleRef: String,
        val pairId: String? = null
    )

    data class WaqafAnnotation(
        val start: Int,
        val end: Int,
        val symbol: String,
        val rule: WaqafRule,
        val pairId: String? = rule.pairId
    )

    enum class ActionCategory(val label: String, val badgeColor: Color) {
        FORBIDDEN("Dilarang Berhenti", QuranColors.BadgeWaqafStop),
        PREFERRED_CONTINUE("Lebih Baik Lanjut", QuranColors.BadgeWaqafContinue),
        MANDATORY("Wajib Berhenti", QuranColors.BadgeWaqafStop),
        PREFERRED_STOP("Lebih Baik Berhenti", QuranColors.BadgeWaqafStop),
        OPTIONAL("Boleh Berhenti / Lanjut", QuranColors.BadgeWaqafOptional)
    }

    val ALL_WAQAF_RULES: List<WaqafRule> = listOf(
        WaqafRule(
            symbol = WAQAF_LA_SYM,
            arabicName = "لا",
            latinName = "Waqaf La",
            meaning = "Jangan berhenti pada tanda ini kecuali pada akhir ayat.",
            recommendation = "Lanjutkan bacaan sampai tanda berhenti berikutnya atau akhir ayat.",
            actionCategory = ActionCategory.FORBIDDEN,
            badgeColor = QuranColors.BadgeWaqafStop,
            detailedRule = "Berhenti di sini dapat memutus makna ayat. Berhenti pada akhir ayat tetap diperbolehkan.",
            exampleAyah = "ذَٰلِكَ ٱلْكِتَٰبُ لَا رَيْبَ ۛ فِيهِ",
            exampleRef = "Al-Baqarah 2:2"
        ),
        WaqafRule(
            symbol = WAQAF_WASHLA_SYM,
            arabicName = "صلى",
            latinName = "Waqaf Wasla",
            meaning = "Lebih baik diteruskan.",
            recommendation = "Utamakan menyambung bacaan; berhenti tetap memiliki ruang pada kebutuhan napas.",
            actionCategory = ActionCategory.PREFERRED_CONTINUE,
            badgeColor = QuranColors.BadgeWaqafContinue,
            detailedRule = "Tanda ini menunjukkan wasal lebih utama daripada berhenti.",
            exampleAyah = "وَأُو۟لَٰٓئِكَ هُمُ ٱلْمُفْلِحُونَ",
            exampleRef = "Al-Baqarah 2:5"
        ),
        WaqafRule(
            symbol = WAQAF_JAIZ_SYM,
            arabicName = "ج",
            latinName = "Waqaf Jaiz",
            meaning = "Boleh berhenti atau meneruskan.",
            recommendation = "Pilih berdasarkan napas dan keterhubungan makna.",
            actionCategory = ActionCategory.OPTIONAL,
            badgeColor = QuranColors.BadgeWaqafOptional,
            detailedRule = "Kedua pilihan bacaan dibolehkan pada tanda ini.",
            exampleAyah = "وَلَهُمْ عَذَابٌ عَظِيمٌۭ",
            exampleRef = "Al-Baqarah 2:7"
        ),
        WaqafRule(
            symbol = WAQAF_MUANAQAH_SYM,
            arabicName = "ۛ ۛ",
            latinName = "Mu'anaqah",
            meaning = "Berhenti pada salah satu dari dua tanda, bukan keduanya.",
            recommendation = "Jika berhenti pada tanda pertama, lanjutkan melewati tanda kedua; begitu juga sebaliknya.",
            actionCategory = ActionCategory.OPTIONAL,
            badgeColor = QuranColors.BadgeWaqafOptional,
            detailedRule = "Dua tanda berpasangan menjaga kesinambungan makna dengan satu kali berhenti.",
            exampleAyah = "ذَٰلِكَ ٱلْكِتَٰبُ لَا رَيْبَ ۛ فِيهِ ۛ هُدًۭى",
            exampleRef = "Al-Baqarah 2:2",
            pairId = "muanaqah"
        ),
        WaqafRule(
            symbol = WAQAF_LAZIM_SYM,
            arabicName = "م",
            latinName = "Waqaf Lazim",
            meaning = "Diharuskan berhenti.",
            recommendation = "Berhenti untuk menjaga makna, kemudian lanjutkan dari kata berikutnya.",
            actionCategory = ActionCategory.MANDATORY,
            badgeColor = QuranColors.BadgeWaqafStop,
            detailedRule = "Tanda mim menunjukkan berhenti yang diperlukan dalam pembacaan standar mushaf.",
            exampleAyah = "وَمِنَ ٱلنَّاسِ مَن يَقُولُ ءَامَنَّا بِٱللَّهِ",
            exampleRef = "Al-Baqarah 2:8"
        ),
        WaqafRule(
            symbol = WAQAF_AWLA_SYM,
            arabicName = "قلى",
            latinName = "Waqaf Qila",
            meaning = "Lebih baik berhenti.",
            recommendation = "Utamakan berhenti, lalu lanjutkan tanpa mengulang jika makna tetap tersambung.",
            actionCategory = ActionCategory.PREFERRED_STOP,
            badgeColor = QuranColors.BadgeWaqafStop,
            detailedRule = "Tanda ini menunjukkan berhenti lebih utama daripada menyambung.",
            exampleAyah = "وَلَٰكِن لَّا يَعْلَمُونَ",
            exampleRef = "Al-Baqarah 2:13"
        ),
        WaqafRule(
            symbol = WAQAF_SAKTAH_SYM,
            arabicName = "س",
            latinName = "Saktah",
            meaning = "Berhenti sejenak tanpa mengambil napas.",
            recommendation = "Tahan suara singkat, tanpa menarik napas, lalu lanjutkan.",
            actionCategory = ActionCategory.OPTIONAL,
            badgeColor = QuranColors.BadgeWaqafOptional,
            detailedRule = "Saktah bukan berhenti panjang; audio hanya boleh diaktifkan bila sumber bacaan tersedia.",
            exampleAyah = "وَقِيلَ مَنْ رَاقٍ",
            exampleRef = "Al-Qiyamah 75:27"
        )
    )

    fun toArabicDigits(number: Int): String {
        val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        return number.toString().map { char ->
            if (char in '0'..'9') arabicDigits[char - '0'] else char
        }.joinToString("")
    }

    fun formatAyahEndMarker(ayahNumber: Int): String {
        require(ayahNumber > 0) { "Nomor ayat harus positif" }
        return " ۝" + toArabicDigits(ayahNumber) + " "
    }

    fun formatAyahTextWithEndMarker(ayahText: String, ayahNumber: Int): String {
        require(ayahNumber > 0) { "Nomor ayat harus positif" }
        val textWithoutTerminalMarker = ayahText
            .replace(Regex("\\s*۝[٠-٩0-9]*\\s*$"), "")
            .trimEnd()
        return textWithoutTerminalMarker + formatAyahEndMarker(ayahNumber)
    }

    /**
     * Adds annotations only when a reviewed catalog can resolve each marker.
     * Glyph presence by itself is insufficient evidence for a semantic rule.
     */
    fun annotateWaqafMarkers(text: AnnotatedString): AnnotatedString {
        if (!SOURCE_CATALOG_VERIFIED) return text
        val builder = AnnotatedString.Builder(text)
        text.text.forEachIndexed { index, char ->
            when {
                char == AYAH_END_SYM.single() -> {
                    builder.addStyle(
                        androidx.compose.ui.text.SpanStyle(color = QuranColors.Secondary),
                        index,
                        index + 1
                    )
                    builder.addStringAnnotation(
                        AYAH_END_ANNOTATION,
                        AYAH_END_SYM,
                        index,
                        index + 1
                    )
                }
                else -> findRuleBySymbol(char)?.let { rule ->
                    builder.addStyle(
                        androidx.compose.ui.text.SpanStyle(color = rule.badgeColor),
                        index,
                        index + 1
                    )
                    builder.addStringAnnotation(WAQAF_ANNOTATION, char.toString(), index, index + 1)
                    rule.pairId?.let { pairId ->
                        builder.addStringAnnotation(
                            WAQAF_PAIR_ANNOTATION,
                            pairId,
                            index,
                            index + 1
                        )
                    }
                }
            }
        }
        return builder.toAnnotatedString()
    }

    fun extractWaqafAnnotations(text: AnnotatedString): List<WaqafAnnotation> =
        text.getStringAnnotations(WAQAF_ANNOTATION, 0, text.length).mapNotNull { annotation ->
            val symbol = annotation.item.singleOrNull()?.toString() ?: return@mapNotNull null
            val rule = findRuleBySymbol(symbol.single()) ?: return@mapNotNull null
            WaqafAnnotation(annotation.start, annotation.end, symbol, rule, rule.pairId)
        }

    fun findRuleBySymbol(char: Char): WaqafRule? {
        if (!SOURCE_CATALOG_VERIFIED) return null
        return ALL_WAQAF_RULES.firstOrNull { it.symbol.contains(char) }
    }
}
