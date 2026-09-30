package com.quranplus.app.features.dzikir.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.quranplus.app.core.ui.theme.QuranColors
import com.quranplus.app.core.utils.TajwidParser

/**
 * Formats Dzikir, Wirid, and Hizib Arabic texts with precise inter-character (antar-huruf)
 * Tajwid color coding consistent with QuranPlus DESIGN.md guidelines.
 */
object DzikirTajwidFormatter {

    data class ColorSpan(val start: Int, val end: Int, val color: Color)

    fun formatArabic(
        arabicText: String,
        tajwidTags: String? = null,
        enableTajwid: Boolean = true,
        baseTextColor: Color = QuranColors.TextArabicDefault
    ): AnnotatedString {
        val cleanArabic = stripTags(arabicText).trim().removeSurrounding("\"")
        if (!enableTajwid || cleanArabic.isBlank()) {
            return buildAnnotatedString {
                withStyle(SpanStyle(color = baseTextColor)) {
                    append(cleanArabic)
                }
            }
        }

        val collectedSpans = mutableListOf<ColorSpan>()

        // 1. Tag-based spans from explicit bracket tags
        val rawTagSource = if (!tajwidTags.isNullOrBlank()) tajwidTags else if (arabicText.contains("[")) arabicText else null
        val tagSource = rawTagSource?.trim()?.removeSurrounding("\"")
        if (tagSource != null) {
            val parsed = TajwidParser.parseBracketTags(tagSource)
            if (parsed.spans.isNotEmpty()) {
                val alignedSpans = TajwidParser.alignSpansToDisplay(parsed, cleanArabic)
                val spansToUse = if (alignedSpans != null && alignedSpans.isNotEmpty()) {
                    alignedSpans
                } else if (cleanArabic.length == parsed.text.length) {
                    parsed.spans
                } else {
                    // Substring offset alignment if cleanArabic contains parsed.text
                    val subIdx = cleanArabic.indexOf(parsed.text)
                    if (subIdx >= 0) {
                        parsed.spans.map { s -> s.copy(start = s.start + subIdx, end = s.end + subIdx) }
                    } else null
                }

                spansToUse?.forEach { span ->
                    span.type.color?.let { color ->
                        collectedSpans += ColorSpan(span.start, span.end, color)
                    }
                }
            }
        }

        // 2. Inter-character rule-based Tajwid detection (antar huruf)
        // Detects Ghunnah, Qalqalah, Mad Wajib/Jaiz, Ikhfa, Idgham, Iqlab directly on cleanArabic
        val detectedRuleSpans = detectInterCharacterRules(cleanArabic)
        detectedRuleSpans.forEach { ruleSpan ->
            // Only add if not already covered by explicit tag spans
            val overlaps = collectedSpans.any { existing ->
                maxOf(existing.start, ruleSpan.start) < minOf(existing.end, ruleSpan.end)
            }
            if (!overlaps) {
                collectedSpans += ruleSpan
            }
        }

        // 3. Build annotated string
        val builder = AnnotatedString.Builder(cleanArabic)
        builder.addStyle(SpanStyle(color = baseTextColor), 0, cleanArabic.length)

        collectedSpans.forEach { span ->
            val safeStart = span.start.coerceIn(0, cleanArabic.length)
            val safeEnd = span.end.coerceIn(safeStart, cleanArabic.length)
            var segStart = -1
            for (i in safeStart until safeEnd) {
                val ch = cleanArabic[i]
                if (ch.isWhitespace() || ch in WAQAF_MARKS) {
                    if (segStart != -1) {
                        builder.addStyle(SpanStyle(color = span.color), segStart, i)
                        segStart = -1
                    }
                } else {
                    if (segStart == -1) {
                        segStart = i
                    }
                }
            }
            if (segStart != -1) {
                builder.addStyle(SpanStyle(color = span.color), segStart, safeEnd)
            }
        }

        return builder.toAnnotatedString()
    }

    private fun detectInterCharacterRules(text: String): List<ColorSpan> {
        val spans = mutableListOf<ColorSpan>()
        val n = text.length
        var i = 0
        while (i < n) {
            val c = text[i]

            // 1. Ghunnah Musyaddadah (Nun / Mim bertasydid)
            if ((c == 'ن' || c == 'م') && i + 1 < n && text[i + 1] == '\u0651') {
                var end = i + 2
                while (end < n && isArabicHarakat(text[end])) end++
                spans += ColorSpan(i, end, QuranColors.TajwidGhunnah)
                i = end
                continue
            }

            // 2. Mad Wajib / Jaiz / Lazim (Maddah Above ~)
            if (c == '\u0653' || (i + 1 < n && text[i + 1] == '\u0653')) {
                val start = if (c == '\u0653') (i - 1).coerceAtLeast(0) else i
                var end = (i + 2).coerceAtMost(n)
                while (end < n && (isArabicHarakat(text[end]) || text[end] in MAD_LETTERS)) end++
                spans += ColorSpan(start, end, QuranColors.TajwidMadWajib)
                i = end
                continue
            }

            // 3. Qalqalah (Qaf, Tha, Ba, Jim, Dal dengan sukun)
            if (c in QALQALAH_LETTERS && i + 1 < n && (text[i + 1] == '\u0652' || text[i + 1] == '\u06E1')) {
                spans += ColorSpan(i, i + 2, QuranColors.TajwidQalqalah)
                i += 2
                continue
            }

            // 4. Nun Sakinah & Tanwin rules (inter-character pair)
            val isNunSakinah = c == 'ن' && i + 1 < n && (text[i + 1] == '\u0652' || text[i + 1] == '\u06E1')
            val isTanwin = c in TANWIN_MARKS
            if (isNunSakinah || isTanwin) {
                val triggerStart = i
                val triggerEnd = if (isNunSakinah) i + 2 else i + 1
                var nextIdx = triggerEnd
                while (nextIdx < n && (text[nextIdx].isWhitespace() || text[nextIdx] in WAQAF_MARKS)) nextIdx++
                if (nextIdx < n) {
                    val targetChar = text[nextIdx]
                    var targetEnd = nextIdx + 1
                    while (targetEnd < n && (text[targetEnd] == '\u0651' || isArabicHarakat(text[targetEnd]))) targetEnd++

                    when {
                        targetChar == 'ب' -> {
                            spans += ColorSpan(triggerStart, triggerEnd, QuranColors.TajwidIqlab)
                            spans += ColorSpan(nextIdx, targetEnd, QuranColors.TajwidIqlab)
                            i = triggerEnd
                            continue
                        }
                        targetChar in IKHFA_LETTERS -> {
                            spans += ColorSpan(triggerStart, triggerEnd, QuranColors.TajwidIkhfa)
                            spans += ColorSpan(nextIdx, targetEnd, QuranColors.TajwidIkhfa)
                            i = triggerEnd
                            continue
                        }
                        targetChar in IDGHAM_BIGHUNNAH_LETTERS -> {
                            spans += ColorSpan(triggerStart, triggerEnd, QuranColors.TajwidIdgham)
                            spans += ColorSpan(nextIdx, targetEnd, QuranColors.TajwidIdgham)
                            i = triggerEnd
                            continue
                        }
                        targetChar in IDGHAM_BILAGHUNNAH_LETTERS -> {
                            spans += ColorSpan(triggerStart, triggerEnd, QuranColors.TajwidIdghamBila)
                            spans += ColorSpan(nextIdx, targetEnd, QuranColors.TajwidIdghamBila)
                            i = triggerEnd
                            continue
                        }
                    }
                }
            }

            // 5. Mim Sakinah rules (inter-character pair)
            if (c == 'م' && i + 1 < n && (text[i + 1] == '\u0652' || text[i + 1] == '\u06E1')) {
                val triggerStart = i
                val triggerEnd = i + 2
                var nextIdx = triggerEnd
                while (nextIdx < n && (text[nextIdx].isWhitespace() || text[nextIdx] in WAQAF_MARKS)) nextIdx++
                if (nextIdx < n) {
                    val targetChar = text[nextIdx]
                    var targetEnd = nextIdx + 1
                    while (targetEnd < n && (text[targetEnd] == '\u0651' || isArabicHarakat(text[targetEnd]))) targetEnd++

                    when (targetChar) {
                        'ب' -> {
                            spans += ColorSpan(triggerStart, triggerEnd, QuranColors.TajwidIkhfaSyafawi)
                            spans += ColorSpan(nextIdx, targetEnd, QuranColors.TajwidIkhfaSyafawi)
                            i = triggerEnd
                            continue
                        }
                        'م' -> {
                            spans += ColorSpan(triggerStart, triggerEnd, QuranColors.TajwidIdghamMimi)
                            spans += ColorSpan(nextIdx, targetEnd, QuranColors.TajwidIdghamMimi)
                            i = triggerEnd
                            continue
                        }
                    }
                }
            }

            i++
        }
        return spans
    }

    private fun isArabicHarakat(ch: Char): Boolean =
        ch in setOf('\u064E', '\u064F', '\u0650', '\u064B', '\u064C', '\u064D', '\u0670', '\u0652', '\u06E1')

    private val QALQALAH_LETTERS = setOf('ق', 'ط', 'ب', 'ج', 'د')
    private val IKHFA_LETTERS = setOf('ت', 'ث', 'ج', 'د', 'ذ', 'ز', 'س', 'ش', 'ص', 'ض', 'ط', 'ظ', 'ف', 'ق', 'ك')
    private val IDGHAM_BIGHUNNAH_LETTERS = setOf('ي', 'ن', 'م', 'و')
    private val IDGHAM_BILAGHUNNAH_LETTERS = setOf('ل', 'ر')
    private val TANWIN_MARKS = setOf('\u064B', '\u064C', '\u064D')
    private val MAD_LETTERS = setOf('ا', 'و', 'ي', 'ى', 'آ')

    private fun stripTags(text: String): String =
        if (text.contains("[")) {
            TajwidParser.parseBracketTags(text).text
        } else {
            text.replace(Regex("""</?tajwid:[a-zA-Z_]+>"""), "")
        }

    private val WAQAF_MARKS = setOf('ۚ', 'ۗ', 'ۖ', 'ۘ', 'ۙ', 'ۛ', 'ۜ', '۞', '۩', '۝')
}
