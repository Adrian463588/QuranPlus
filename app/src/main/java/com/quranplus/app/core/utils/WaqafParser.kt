package com.quranplus.app.core.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import com.quranplus.app.core.ui.theme.QuranColors

/**
 * Waqaf marker formatting and provenance gate.
 *
 * Marker semantics are not inferred from a glyph. The reviewed Waqaf catalog
 * is not present in this checkout, so semantic annotations and guide content
 * stay disabled until a source manifest is approved.
 */
object WaqafParser {

    const val SOURCE_CATALOG_VERIFIED = false
    const val WAQAF_ANNOTATION = "quranplus_waqaf"

    const val WAQAF_LA_SYM = "ۘ"
    const val WAQAF_JAIZ_SYM = "ۚ"
    const val WAQAF_WASHLA_SYM = "ۖ"
    const val WAQAF_AWLA_SYM = "ۗ"
    const val WAQAF_MUANAQAH_SYM = "ۛ"
    const val WAQAF_SAKTAH_SYM = "ۜ"
    const val WAQAF_LAZIM_SYM = "ۙ"
    const val AYAH_END_SYM = "۝"

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

    /** Empty until each rule and example has a pinned, reviewed source. */
    val ALL_WAQAF_RULES: List<WaqafRule> = emptyList()

    fun toArabicDigits(number: Int): String {
        val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        return number.toString().map { char ->
            if (char in '0'..'9') arabicDigits[char - '0'] else char
        }.joinToString("")
    }

    fun formatAyahEndMarker(ayahNumber: Int): String {
        require(ayahNumber > 0) { "Nomor ayat harus positif" }
        return " ۝${toArabicDigits(ayahNumber)} "
    }

    /**
     * Adds annotations only when a reviewed catalog can resolve each marker.
     * Glyph presence by itself is insufficient evidence for a semantic rule.
     */
    fun annotateWaqafMarkers(text: AnnotatedString): AnnotatedString {
        if (!SOURCE_CATALOG_VERIFIED) return text
        val builder = AnnotatedString.Builder(text)
        text.text.forEachIndexed { index, char ->
            findRuleBySymbol(char)?.let {
                builder.addStringAnnotation(WAQAF_ANNOTATION, char.toString(), index, index + 1)
            }
        }
        return builder.toAnnotatedString()
    }

    /** Returns null until source-backed rule mapping is available. */
    fun findRuleBySymbol(char: Char): WaqafRule? {
        if (!SOURCE_CATALOG_VERIFIED) return null
        return ALL_WAQAF_RULES.firstOrNull { it.symbol.contains(char) }
    }
}
