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
        if (!enableTajwid) {
            val clean = stripTags(arabicText)
            return buildAnnotatedString {
                withStyle(SpanStyle(color = baseTextColor)) {
                    append(clean)
                }
            }
        }

        // If explicit bracket tags are supplied (e.g. "[g[إِنَّ] [f[مِن قَبْلِ]]")
        val tagSource = if (!tajwidTags.isNullOrBlank()) tajwidTags else if (arabicText.contains("[")) arabicText else null
        if (tagSource != null) {
            val parsed = TajwidParser.parseBracketTags(tagSource)
            if (parsed.spans.isNotEmpty()) {
                val targetText = if (arabicText.isNotBlank() && !arabicText.contains("[") && arabicText.length == parsed.text.length) {
                    arabicText
                } else {
                    parsed.text
                }
                val builder = AnnotatedString.Builder(targetText)
                builder.addStyle(SpanStyle(color = baseTextColor), 0, targetText.length)
                parsed.spans.forEach { span ->
                    span.type.color?.let { color ->
                        val safeStart = span.start.coerceIn(0, targetText.length)
                        val safeEnd = span.end.coerceIn(safeStart, targetText.length)
                        var segStart = -1
                        for (i in safeStart until safeEnd) {
                            val ch = targetText[i]
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

        // Fallback to TajwidParser native pipeline
        return TajwidParser.buildColoredAyahText(
            arabicText = arabicText,
            tajwidTags = tajwidTags,
            enableTajwid = enableTajwid,
            baseTextColor = baseTextColor
        )
    }

    private fun stripTags(text: String): String =
        if (text.contains("[")) {
            TajwidParser.parseBracketTags(text).text
        } else {
            text.replace(Regex("""</?tajwid:[a-zA-Z_]+>"""), "")
        }

    private val WAQAF_MARKS = setOf('ۚ', 'ۗ', 'ۖ', 'ۘ', 'ۙ', 'ۛ', 'ۜ', '۞', '۩', '۝')
}

