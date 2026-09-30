package com.quranplus.app.features.dzikir.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.quranplus.app.core.ui.theme.QuranColors
import com.quranplus.app.core.utils.TajwidParser

/**
 * Formats Dzikir, Wirid, and Hizib Arabic texts with precise Tajwid color coding
 * consistent with QuranPlus DESIGN.md guidelines.
 */
object DzikirTajwidFormatter {

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

        // If explicit bracket tags are supplied (e.g. "[g[إِنَّ] [f[مِن قَبْلِ]]")
        val rawTagSource = if (!tajwidTags.isNullOrBlank()) tajwidTags else if (arabicText.contains("[")) arabicText else null
        val tagSource = rawTagSource?.trim()?.removeSurrounding("\"")
        if (tagSource != null) {
            val parsed = TajwidParser.parseBracketTags(tagSource)
            if (parsed.spans.isNotEmpty() && !parsed.malformed) {
                val alignedSpans = TajwidParser.alignSpansToDisplay(parsed, cleanArabic)
                val spansToUse = if (alignedSpans != null && alignedSpans.isNotEmpty()) {
                    alignedSpans
                } else if (cleanArabic.length == parsed.text.length) {
                    parsed.spans
                } else null

                if (spansToUse != null) {
                    val builder = AnnotatedString.Builder(cleanArabic)
                    builder.addStyle(SpanStyle(color = baseTextColor), 0, cleanArabic.length)
                    spansToUse.forEach { span ->
                        span.type.color?.let { color ->
                            val safeStart = span.start.coerceIn(0, cleanArabic.length)
                            val safeEnd = span.end.coerceIn(safeStart, cleanArabic.length)
                            var segStart = -1
                            for (i in safeStart until safeEnd) {
                                val ch = cleanArabic[i]
                                if (ch.isWhitespace() || ch in WAQAF_MARKS) {
                                    if (segStart != -1) {
                                        builder.addStyle(SpanStyle(color = color), segStart, i)
                                        segStart = -1
                                    }
                                } else {
                                    if (segStart == -1) {
                                        segStart = i
                                    }
                                }
                            }
                            if (segStart != -1) {
                                builder.addStyle(SpanStyle(color = color), segStart, safeEnd)
                            }
                        }
                    }
                    return builder.toAnnotatedString()
                }
            }
        }

        // Fallback: render cleanArabic safely with base color, preserving 100% harakat
        return buildAnnotatedString {
            withStyle(SpanStyle(color = baseTextColor)) {
                append(cleanArabic)
            }
        }
    }

    private fun stripTags(text: String): String =
        if (text.contains("[")) {
            TajwidParser.parseBracketTags(text).text
        } else {
            text.replace(Regex("""</?tajwid:[a-zA-Z_]+>"""), "")
        }

    private val WAQAF_MARKS = setOf('ۚ', 'ۗ', 'ۖ', 'ۘ', 'ۙ', 'ۛ', 'ۜ', '۞', '۩', '۝')
}

