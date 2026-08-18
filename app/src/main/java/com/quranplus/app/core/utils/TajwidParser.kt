package com.quranplus.app.core.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.quranplus.app.core.ui.theme.QuranColors

/**
 * Tajwid Parser and Color Formatter
 * Implements Tajwid Color Coding conforming to DESIGN.md using AnnotatedString.
 */
object TajwidParser {

    enum class TajwidType(val label: String, val color: Color, val description: String) {
        IDGHAM("Idgham", QuranColors.TajwidIdgham, "Memasukkan bunyi huruf (Hijau)"),
        IKHFA("Ikhfa", QuranColors.TajwidIkhfa, "Menyamarkan bacaan nun/tanwin (Biru)"),
        IQLAB("Iqlab", QuranColors.TajwidIqlab, "Mengubah nun/tanwin menjadi mim (Merah)"),
        QALQALAH("Qalqalah", QuranColors.TajwidQalqalah, "Memantulkan huruf qalqalah (Oranye)"),
        MAD("Mad", QuranColors.TajwidMad, "Memanjangkan bacaan mad (Ungu)"),
        GHUNNAH("Ghunnah", QuranColors.TajwidGhunnah, "Mendengungkan mim/nun tasydid (Merah Muda)")
    }

    // Arabic Character sets for rule detection
    private val QALQALAH_CHARS = setOf('ق', 'ط', 'ب', 'ج', 'د')
    private val IQLAB_CHAR = 'م'
    private val SUKUN = '\u0652'
    private val SHADDA = '\u0651'
    private val FATHATAN = '\u064B'
    private val DAMMATAN = '\u064C'
    private val KASRATAN = '\u064D'
    private val MAD_CHARS = setOf('ا', 'و', 'ي', 'ى', 'آ', 'ء')
    private val MAD_MARKS = setOf('\u0653', '\u0670', '\u0656', '\u06E5', '\u06E6') // Maddah, Dagger alif

    /**
     * Parses Arabic text with optional tajwid tags or applies phonetic rule highlighting.
     * Supports both tagged format `<tajwid:idgham>text</tajwid>` and phonetic heuristic styling.
     */
    fun buildColoredAyahText(
        arabicText: String,
        tajwidTags: String? = null,
        enableTajwid: Boolean = true,
        baseTextColor: Color = QuranColors.OnSurfaceDark
    ): AnnotatedString {
        if (!enableTajwid) {
            return AnnotatedString(arabicText)
        }

        // If explicitly tagged XML-like structure exists
        if (arabicText.contains("<tajwid:") || (tajwidTags != null && tajwidTags.isNotBlank())) {
            return parseTaggedArabic(arabicText, baseTextColor)
        }

        // Fallback: Apply linguistic rule parsing on Arabic Uthmani string
        return parseUthmaniScript(arabicText, baseTextColor)
    }

    private fun parseTaggedArabic(text: String, defaultColor: Color): AnnotatedString {
        return buildAnnotatedString {
            var index = 0
            val tagRegex = Regex("""<tajwid:([a-zA-Z]+)>(.*?)</tajwid>""")
            val matches = tagRegex.findAll(text)

            for (match in matches) {
                if (match.range.first > index) {
                    withStyle(SpanStyle(color = defaultColor)) {
                        append(text.substring(index, match.range.first))
                    }
                }

                val typeStr = match.groupValues[1].uppercase()
                val content = match.groupValues[2]
                val tajwidType = when (typeStr) {
                    "IDGHAM" -> TajwidType.IDGHAM
                    "IKHFA" -> TajwidType.IKHFA
                    "IQLAB" -> TajwidType.IQLAB
                    "QALQALAH" -> TajwidType.QALQALAH
                    "MAD" -> TajwidType.MAD
                    "GHUNNAH" -> TajwidType.GHUNNAH
                    else -> null
                }

                if (tajwidType != null) {
                    withStyle(SpanStyle(color = tajwidType.color)) {
                        append(content)
                    }
                } else {
                    withStyle(SpanStyle(color = defaultColor)) {
                        append(content)
                    }
                }
                index = match.range.last + 1
            }

            if (index < text.length) {
                withStyle(SpanStyle(color = defaultColor)) {
                    append(text.substring(index))
                }
            }
        }
    }

    /**
     * Heuristic tokenizer for Uthmani diacritics
     */
    private fun parseUthmaniScript(text: String, defaultColor: Color): AnnotatedString {
        return buildAnnotatedString {
            var i = 0
            while (i < text.length) {
                val char = text[i]

                // Check for Shadda on Nun or Mim -> Ghunnah
                if ((char == 'ن' || char == 'م') && i + 1 < text.length && text[i + 1] == SHADDA) {
                    withStyle(SpanStyle(color = TajwidType.GHUNNAH.color)) {
                        append(char)
                        append(SHADDA)
                    }
                    i += 2
                    continue
                }

                // Check for Maddah diacritic or long mad
                if (char in MAD_MARKS || (char in MAD_CHARS && i + 1 < text.length && text[i + 1] == '\u0653')) {
                    withStyle(SpanStyle(color = TajwidType.MAD.color)) {
                        append(char)
                        if (i + 1 < text.length && text[i + 1] == '\u0653') {
                            append(text[i + 1])
                            i++
                        }
                    }
                    i++
                    continue
                }

                // Check for Qalqalah (Qaf, Tha, Ba, Jim, Dal with sukun or at pause)
                if (char in QALQALAH_CHARS && i + 1 < text.length && text[i + 1] == SUKUN) {
                    withStyle(SpanStyle(color = TajwidType.QALQALAH.color)) {
                        append(char)
                        append(SUKUN)
                    }
                    i += 2
                    continue
                }

                // Check for Tanwin -> Ikhfa / Idgham markers
                if (char == FATHATAN || char == DAMMATAN || char == KASRATAN) {
                    withStyle(SpanStyle(color = TajwidType.IKHFA.color)) {
                        append(char)
                    }
                    i++
                    continue
                }

                // Default plain character
                withStyle(SpanStyle(color = defaultColor)) {
                    append(char)
                }
                i++
            }
        }
    }
}
