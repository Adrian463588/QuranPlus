package com.quranplus.app.core.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.quranplus.app.core.ui.theme.QuranColors

/**
 * Tajwid Parser and Color Formatter
 * Implements precise character & letter-level Tajwid Color Coding conforming to DESIGN.md.
 * Accurately colors only the specific Arabic letters and diacritics involved in each Tajwid rule.
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

    // Diacritic & Special Unicode constants
    private const val SUKUN          = '\u0652'
    private const val QURANIC_SUKUN  = '\u06E1' // ۡ Small High Dotless Head of Khah
    private const val SHADDA         = '\u0651'
    private const val FATHATAN       = '\u064B'
    private const val DAMMATAN       = '\u064C'
    private const val KASRATAN       = '\u064D'
    private const val FATHA          = '\u064E'
    private const val DAMMA          = '\u064F'
    private const val KASRA          = '\u0650'
    private const val MADDAH         = '\u0653' // ٓ Maddah Above
    private const val SMALL_MADDAH   = '\u06E4' // ۤ Small High Madda
    private const val DAGGER_ALIF    = '\u0670' // ٰ Superscript Alif
    private const val SMALL_HIGH_MEEM= '\u06E2' // ۢ Small High Meem (Iqlab marker)
    private const val SMALL_LOW_MEEM = '\u06ED' // ۭ Small Low Meem
    private const val SMALL_WAW      = '\u06E5' // ۥ Small Waw
    private const val SMALL_YA       = '\u06E6' // ۦ Small Ya

    // Arabic Base letter constants
    private const val NUN            = '\u0646' // ن
    private const val MIM            = '\u0645' // م
    private const val BA             = '\u0628' // ب
    private const val ALIF           = '\u0627' // ا
    private const val ALIF_WASLA     = '\u0671' // ٱ
    private const val WAW            = '\u0648' // و
    private const val YA             = '\u064A' // ي
    private const val ALIF_MAQSURA   = '\u0649' // ى
    private const val ALIF_MADDA     = '\u0622' // آ

    // Tajwid letter sets
    private val QALQALAH_LETTERS = setOf('\u0642', '\u0637', '\u0628', '\u062C', '\u062F') // ق ط ب ج د
    private val IDGHAM_LETTERS   = setOf('\u064A', '\u0646', '\u0645', '\u0648', '\u0644', '\u0631') // ي ن م و ل ر
    private val IKHFA_LETTERS    = setOf(
        '\u062A','\u062B','\u062C','\u062F','\u0630','\u0632','\u0633',
        '\u0634','\u0635','\u0636','\u0637','\u0638','\u0641','\u0642','\u0643'
    ) // ت ث ج د ذ ز س ش ص ض ط ظ ف ق ك

    private data class Span(val start: Int, val end: Int, val color: Color)

    /**
     * Parses Arabic ayah text and returns an [AnnotatedString] with precise letter-level Tajwid color spans.
     */
    fun buildColoredAyahText(
        arabicText: String,
        tajwidTags: String? = null,
        enableTajwid: Boolean = true,
        baseTextColor: Color = QuranColors.OnSurfaceDark
    ): AnnotatedString {
        if (!enableTajwid || arabicText.isBlank()) {
            return AnnotatedString(arabicText)
        }

        // If text contains inline tags like <tajwid:idgham>...
        if (arabicText.contains("<tajwid:") || (!tajwidTags.isNullOrBlank() && arabicText.contains("<"))) {
            return parseTaggedArabic(arabicText, baseTextColor)
        }

        // Letter-level precise heuristic scanner
        return parseLetterLevel(arabicText, baseTextColor)
    }

    // ─── Tagged XML parser ───────────────────────────────────────────────────
    private fun parseTaggedArabic(text: String, defaultColor: Color): AnnotatedString {
        return buildAnnotatedString {
            var index = 0
            val tagRegex = Regex("""<tajwid:([a-zA-Z]+)>(.*?)</tajwid>""")
            for (match in tagRegex.findAll(text)) {
                if (match.range.first > index) {
                    withStyle(SpanStyle(color = defaultColor)) { append(text.substring(index, match.range.first)) }
                }
                val t = when (match.groupValues[1].uppercase()) {
                    "IDGHAM"    -> TajwidType.IDGHAM
                    "IKHFA"     -> TajwidType.IKHFA
                    "IQLAB"     -> TajwidType.IQLAB
                    "QALQALAH"  -> TajwidType.QALQALAH
                    "MAD"       -> TajwidType.MAD
                    "GHUNNAH"   -> TajwidType.GHUNNAH
                    else        -> null
                }
                withStyle(SpanStyle(color = t?.color ?: defaultColor)) { append(match.groupValues[2]) }
                index = match.range.last + 1
            }
            if (index < text.length) {
                withStyle(SpanStyle(color = defaultColor)) { append(text.substring(index)) }
            }
        }
    }

    // ─── Letter-Level Precise Scanner ─────────────────────────────────────────
    private fun parseLetterLevel(text: String, defaultColor: Color): AnnotatedString {
        val spans = mutableListOf<Span>()
        val len = text.length
        var i = 0

        while (i < len) {
            val c = text[i]

            // 1. Check for Dagger Alif (ٰ), Small Waw (ۥ), Small Ya (ۦ) -> Mad
            if (c == DAGGER_ALIF || c == SMALL_WAW || c == SMALL_YA) {
                spans.add(Span(i, i + 1, TajwidType.MAD.color))
                i++
                continue
            }

            // 2. Check for Maddah diacritic (ٓ or ۤ) -> Mad (color the preceding letter + maddah)
            if (c == MADDAH || c == SMALL_MADDAH) {
                val start = if (i > 0 && isArabicLetter(text[i - 1])) i - 1 else i
                val end = findDiacriticEnd(text, i + 1)
                spans.add(Span(start, end, TajwidType.MAD.color))
                i = end
                continue
            }

            // 3. Check for Alif-Madda (آ)
            if (c == ALIF_MADDA) {
                val end = findDiacriticEnd(text, i + 1)
                spans.add(Span(i, end, TajwidType.MAD.color))
                i = end
                continue
            }

            // 4. Check for Small High/Low Meem (Iqlab marker ۢ or ۭ)
            if (c == SMALL_HIGH_MEEM || c == SMALL_LOW_MEEM) {
                val start = if (i > 0) i - 1 else i
                spans.add(Span(start, i + 1, TajwidType.IQLAB.color))
                i++
                continue
            }

            // If this is an Arabic base letter:
            if (isArabicLetter(c)) {
                val diacriticEnd = findDiacriticEnd(text, i + 1)
                val diacritics = text.substring(i + 1, diacriticEnd)

                // ── Rule A: Ghunnah ── Nun or Mim with Shadda (نّ or مّ)
                if ((c == NUN || c == MIM) && diacritics.contains(SHADDA)) {
                    spans.add(Span(i, diacriticEnd, TajwidType.GHUNNAH.color))
                    i = diacriticEnd
                    continue
                }

                // ── Rule B: Qalqalah ── (ق ط ب ج د) with Sukun
                if (c in QALQALAH_LETTERS && (diacritics.contains(SUKUN) || diacritics.contains(QURANIC_SUKUN))) {
                    spans.add(Span(i, diacriticEnd, TajwidType.QALQALAH.color))
                    i = diacriticEnd
                    continue
                }

                // ── Rule C: Nun Sukun / Tanwin rules (Iqlab, Idgham, Ikhfa) ──
                val isNunSukun = (c == NUN && (diacritics.contains(SUKUN) || diacritics.contains(QURANIC_SUKUN) || diacritics.isEmpty()))
                val hasTanwin = diacritics.any { it == FATHATAN || it == DAMMATAN || it == KASRATAN }

                if (isNunSukun || hasTanwin) {
                    val nextLetterIdx = findNextArabicLetter(text, diacriticEnd)
                    if (nextLetterIdx != -1) {
                        val nextLetter = text[nextLetterIdx]
                        val nextDiacriticEnd = findDiacriticEnd(text, nextLetterIdx + 1)

                        when {
                            // Iqlab: Nun/Tanwin followed by Ba (ب)
                            nextLetter == BA -> {
                                spans.add(Span(i, diacriticEnd, TajwidType.IQLAB.color))
                                spans.add(Span(nextLetterIdx, nextDiacriticEnd, TajwidType.IQLAB.color))
                                i = diacriticEnd
                                continue
                            }
                            // Idgham: Nun/Tanwin followed by Yarmalun (ي ن م و ل ر)
                            nextLetter in IDGHAM_LETTERS -> {
                                spans.add(Span(i, diacriticEnd, TajwidType.IDGHAM.color))
                                spans.add(Span(nextLetterIdx, nextDiacriticEnd, TajwidType.IDGHAM.color))
                                i = diacriticEnd
                                continue
                            }
                            // Ikhfa: Nun/Tanwin followed by Ikhfa letters
                            nextLetter in IKHFA_LETTERS -> {
                                spans.add(Span(i, diacriticEnd, TajwidType.IKHFA.color))
                                spans.add(Span(nextLetterIdx, nextDiacriticEnd, TajwidType.IKHFA.color))
                                i = diacriticEnd
                                continue
                            }
                        }
                    }
                }

                // ── Rule D: Natural Mad (Alif after Fatha, Waw after Damma, Ya after Kasra) ──
                val prevLetterIdx = findPrevArabicLetter(text, i)
                if (prevLetterIdx != -1) {
                    val prevDiacritics = text.substring(prevLetterIdx + 1, i)
                    val isAlifMad = (c == ALIF || c == ALIF_MAQSURA) && prevDiacritics.contains(FATHA) && !diacritics.contains(SHADDA) && !diacritics.contains(SUKUN)
                    val isWawMad = c == WAW && prevDiacritics.contains(DAMMA) && (diacritics.contains(SUKUN) || diacritics.contains(QURANIC_SUKUN) || diacritics.isEmpty())
                    val isYaMad = (c == YA || c == ALIF_MAQSURA) && prevDiacritics.contains(KASRA) && (diacritics.contains(SUKUN) || diacritics.contains(QURANIC_SUKUN) || diacritics.isEmpty())

                    if (isAlifMad || isWawMad || isYaMad) {
                        spans.add(Span(i, diacriticEnd, TajwidType.MAD.color))
                        i = diacriticEnd
                        continue
                    }
                }

                i = diacriticEnd
                continue
            }

            i++
        }

        // Build the AnnotatedString with default color and overlay Tajwid spans
        val builder = AnnotatedString.Builder(text)
        builder.addStyle(SpanStyle(color = defaultColor), 0, text.length)
        for (span in spans) {
            if (span.start in 0 until len && span.end in (span.start + 1)..len) {
                builder.addStyle(SpanStyle(color = span.color), span.start, span.end)
            }
        }
        return builder.toAnnotatedString()
    }

    // ─── Helper Functions ─────────────────────────────────────────────────────

    private fun isArabicLetter(c: Char): Boolean =
        c.code in 0x0621..0x064A || c == ALIF_WASLA || c == ALIF_MAQSURA || c == ALIF_MADDA

    private fun isDiacritic(c: Char): Boolean =
        c.code in 0x064B..0x065F || c == DAGGER_ALIF || c == MADDAH || c == SMALL_MADDAH ||
        c == QURANIC_SUKUN || c == SMALL_HIGH_MEEM || c == SMALL_LOW_MEEM || c == SMALL_WAW || c == SMALL_YA

    private fun findDiacriticEnd(text: String, from: Int): Int {
        var idx = from
        while (idx < text.length && isDiacritic(text[idx])) {
            idx++
        }
        return idx
    }

    private fun findNextArabicLetter(text: String, from: Int): Int {
        var idx = from
        while (idx < text.length) {
            val c = text[idx]
            if (isArabicLetter(c)) return idx
            // Stop at word boundaries if too far (e.g. more than 1 space or punctuation)
            if (c == '\n' || c == 'ۖ' || c == 'ۗ' || c == 'ۚ' || c == 'ۛ' || c == 'ۜ' || c == '۝') return -1
            idx++
        }
        return -1
    }

    private fun findPrevArabicLetter(text: String, before: Int): Int {
        var idx = before - 1
        while (idx >= 0) {
            if (isArabicLetter(text[idx])) return idx
            if (text[idx] == ' ' || text[idx] == '\n') return -1
            idx--
        }
        return -1
    }
}
