package com.quranplus.app.core.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.quranplus.app.core.ui.theme.QuranColors

/**
 * Tajwid Parser and Color Formatter
 * Implements precise two-letter (inter-character pair) Tajwid Color Coding conforming to DESIGN.md & PRD.md.
 * Accurately colors both the triggering letter (Nun Sukun, Tanwin, Mim Sukun) and the recipient letter
 * as well as Ghunnah, Qalqalah, and Mad.
 */
object TajwidParser {

    enum class TajwidType(
        val label: String,
        val color: Color,
        val harakatDuration: String,
        val description: String,
        val ruleExplanation: String
    ) {
        GHUNNAH(
            label = "Ghunnah Musyaddadah",
            color = QuranColors.TajwidGhunnah,
            harakatDuration = "2-3 Harakat",
            description = "Nun & Mim bertasydid",
            ruleExplanation = "Nun atau Mim bertasydid dibaca dengan dengung sempurna yang ditahan selama 2-3 harakat."
        ),
        IDGHAM_BIGHUNNAH(
            label = "Idgham Bighunnah",
            color = QuranColors.TajwidIdgham,
            harakatDuration = "2 Harakat",
            description = "Nun mati/tanwin bertemu ي ن م و",
            ruleExplanation = "Nun mati atau tanwin melebur ke huruf berikutnya disertai dengung selama 2 harakat."
        ),
        IDGHAM_BILAGHUNNAH(
            label = "Idgham Bilaghunnah",
            color = QuranColors.TajwidIdghamBila,
            harakatDuration = "1-2 Harakat",
            description = "Nun mati/tanwin bertemu ل ر",
            ruleExplanation = "Nun mati atau tanwin melebur sempurna ke dalam Lam atau Ra tanpa dengung."
        ),
        IDGHAM_MIM_MIMI(
            label = "Idgham Mitslain / Mimi",
            color = QuranColors.TajwidIdghamMimi,
            harakatDuration = "2 Harakat",
            description = "Mim mati bertemu Mim",
            ruleExplanation = "Mim mati melebur ke dalam Mim berharakat berikutnya disertai dengung 2 harakat."
        ),
        IQLAB(
            label = "Iqlab",
            color = QuranColors.TajwidIqlab,
            harakatDuration = "2 Harakat",
            description = "Nun mati/tanwin bertemu ب",
            ruleExplanation = "Bunyi Nun mati atau tanwin diganti menjadi bunyi Mim samar disertai dengung 2 harakat sebelum melafalkan Ba."
        ),
        IKHFA_HAQIQI(
            label = "Ikhfa Haqiqi",
            color = QuranColors.TajwidIkhfa,
            harakatDuration = "2 Harakat",
            description = "Nun mati/tanwin bertemu 15 huruf ikhfa",
            ruleExplanation = "Nun mati atau tanwin disamarkan antara Izhar dan Idgham dengan dengung 2 harakat."
        ),
        IKHFA_SYAFAWI(
            label = "Ikhfa Syafawi",
            color = QuranColors.TajwidIkhfaSyafawi,
            harakatDuration = "2 Harakat",
            description = "Mim mati bertemu ب",
            ruleExplanation = "Mim mati disamarkan di kedua bibir disertai dengung 2 harakat saat bertemu huruf Ba."
        ),
        QALQALAH(
            label = "Qalqalah",
            color = QuranColors.TajwidQalqalah,
            harakatDuration = "Pantulan (Sughra/Kubra)",
            description = "Huruf ق ط ب ج د sukun / waqaf",
            ruleExplanation = "Huruf Qalqalah dipantulkan bunyinya saat berharakat sukun di tengah (Sughra) atau saat berhenti di akhir kata/ayat (Kubra)."
        ),
        IZHAR_HALQI(
            label = "Izhar Halqi / Syafawi",
            color = QuranColors.TajwidIzhar,
            harakatDuration = "Jelas (Tanpa Dengung)",
            description = "Nun/Mim mati dibaca jelas",
            ruleExplanation = "Dibaca jelas, tegas, tanpa menambah dengung atau menahan suara."
        ),
        MAD_TABII(
            label = "Mad Tabi'i / Asli",
            color = QuranColors.TajwidMad,
            harakatDuration = "2 Harakat",
            description = "Panjang 2 harakat",
            ruleExplanation = "Mad asli dengan memanjangkan suara sepanjang 2 harakat (1 alif) secara stabil."
        ),
        MAD_WAJIB_JAIZ(
            label = "Mad Wajib / Jaiz",
            color = QuranColors.TajwidMadWajib,
            harakatDuration = "4-5 Harakat",
            description = "Panjang 4-5 harakat (tanda bendera ~)",
            ruleExplanation = "Mad bertemu hamzah dalam satu kata (Wajib Muttashil) atau kata terpisah (Jaiz Munfashil)."
        ),
        MAD_LAZIM(
            label = "Mad Lazim / Farq",
            color = QuranColors.TajwidMadLazim,
            harakatDuration = "6 Harakat (Wajib)",
            description = "Panjang 6 harakat wajib",
            ruleExplanation = "Mad bertemu huruf bertasydid atau sukun lazim, wajib dipanjangkan 6 harakat penuh."
        )
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
    private const val LAM            = '\u0644' // ل
    private const val RA             = '\u0631' // ر

    // Tajwid letter sets
    private val QALQALAH_LETTERS = setOf('\u0642', '\u0637', '\u0628', '\u062C', '\u062F') // ق ط ب ج د
    private val IDGHAM_BIGHUNNAH_LETTERS = setOf('\u064A', '\u0646', '\u0645', '\u0648') // ي ن م و
    private val IDGHAM_BILAGHUNNAH_LETTERS = setOf('\u0644', '\u0631') // ل ر
    private val IKHFA_LETTERS = setOf(
        '\u062A','\u062B','\u062C','\u062F','\u0630','\u0632','\u0633',
        '\u0634','\u0635','\u0636','\u0637','\u0638','\u0641','\u0642','\u0643'
    ) // ت ث ج د ذ ز س ش ص ض ط ظ ف ق ك
    private val IZHAR_HALQI_LETTERS = setOf('\u0621', '\u0647', '\u0639', '\u062D', '\u063A', '\u062E', '\u0623', '\u0625', '\u0624', '\u0626') // ء هـ ع ح غ خ

    data class TajwidSpan(
        val start: Int,
        val end: Int,
        val type: TajwidType,
        val snippet: String = ""
    )

    /**
     * Parses Arabic ayah text and returns an [AnnotatedString] with precise two-letter (inter-character pair) Tajwid color spans.
     */
    fun buildColoredAyahText(
        arabicText: String,
        tajwidTags: String? = null,
        enableTajwid: Boolean = true,
        baseTextColor: Color = QuranColors.TextArabicDefault
    ): AnnotatedString {
        if (!enableTajwid || arabicText.isBlank()) {
            return AnnotatedString(arabicText)
        }

        // If text contains inline tags like <tajwid:idgham>...
        if (arabicText.contains("<tajwid:") || (!tajwidTags.isNullOrBlank() && arabicText.contains("<"))) {
            return parseTaggedArabic(arabicText, baseTextColor)
        }

        // Two-letter inter-character scanner
        val spans = detectTajwidSpans(arabicText)
        val builder = AnnotatedString.Builder(arabicText)
        builder.addStyle(SpanStyle(color = baseTextColor), 0, arabicText.length)

        for (span in spans) {
            if (span.start in 0 until arabicText.length && span.end in (span.start + 1)..arabicText.length) {
                builder.addStyle(SpanStyle(color = span.type.color), span.start, span.end)
            }
        }
        return builder.toAnnotatedString()
    }

    /**
     * Extracts all detected Tajwid occurrences with their full details for bottom sheet display.
     */
    fun extractTajwidOccurrences(arabicText: String): List<TajwidSpan> {
        if (arabicText.isBlank()) return emptyList()
        return detectTajwidSpans(arabicText)
    }

    // ─── Tagged XML parser ───────────────────────────────────────────────────
    private fun parseTaggedArabic(text: String, defaultColor: Color): AnnotatedString {
        return buildAnnotatedString {
            var index = 0
            val tagRegex = Regex("""<tajwid:([a-zA-Z_]+)>(.*?)</tajwid>""")
            for (match in tagRegex.findAll(text)) {
                if (match.range.first > index) {
                    withStyle(SpanStyle(color = defaultColor)) { append(text.substring(index, match.range.first)) }
                }
                val typeName = match.groupValues[1].uppercase()
                val t = when {
                    typeName.contains("GHUNNAH") -> TajwidType.GHUNNAH
                    typeName.contains("IDGHAM_BILA") || typeName.contains("BILAGHUNNAH") -> TajwidType.IDGHAM_BILAGHUNNAH
                    typeName.contains("IDGHAM_MIMI") || typeName.contains("MITSLAIN") -> TajwidType.IDGHAM_MIM_MIMI
                    typeName.contains("IDGHAM") -> TajwidType.IDGHAM_BIGHUNNAH
                    typeName.contains("IQLAB") -> TajwidType.IQLAB
                    typeName.contains("IKHFA_SYAFAWI") -> TajwidType.IKHFA_SYAFAWI
                    typeName.contains("IKHFA") -> TajwidType.IKHFA_HAQIQI
                    typeName.contains("QALQALAH") -> TajwidType.QALQALAH
                    typeName.contains("MAD_LAZIM") || typeName.contains("FARQ") -> TajwidType.MAD_LAZIM
                    typeName.contains("MAD_WAJIB") || typeName.contains("MAD_JAIZ") || typeName.contains("MADDAH") -> TajwidType.MAD_WAJIB_JAIZ
                    typeName.contains("MAD") -> TajwidType.MAD_TABII
                    typeName.contains("IZHAR") -> TajwidType.IZHAR_HALQI
                    else -> null
                }
                withStyle(SpanStyle(color = t?.color ?: defaultColor)) { append(match.groupValues[2]) }
                index = match.range.last + 1
            }
            if (index < text.length) {
                withStyle(SpanStyle(color = defaultColor)) { append(text.substring(index)) }
            }
        }
    }

    // ─── Two-Letter Inter-Character Heuristic Scanner ─────────────────────────
    private fun detectTajwidSpans(text: String): List<TajwidSpan> {
        val spans = mutableListOf<TajwidSpan>()
        val len = text.length
        var i = 0

        while (i < len) {
            val c = text[i]

            // 1. Dagger Alif (ٰ), Small Waw (ۥ), Small Ya (ۦ) -> Mad Tabi'i
            if (c == DAGGER_ALIF || c == SMALL_WAW || c == SMALL_YA) {
                val start = if (i > 0 && isArabicLetter(text[i - 1])) i - 1 else i
                spans.add(TajwidSpan(start, i + 1, TajwidType.MAD_TABII, text.substring(start, i + 1)))
                i++
                continue
            }

            // 2. Maddah Above (ٓ or ۤ) -> Mad Wajib / Jaiz (4-5 Harakat)
            if (c == MADDAH || c == SMALL_MADDAH) {
                val start = if (i > 0 && isArabicLetter(text[i - 1])) i - 1 else i
                val end = findDiacriticEnd(text, i + 1)
                spans.add(TajwidSpan(start, end, TajwidType.MAD_WAJIB_JAIZ, text.substring(start, end)))
                i = end
                continue
            }

            // 3. Alif-Madda (آ) -> Mad
            if (c == ALIF_MADDA) {
                val end = findDiacriticEnd(text, i + 1)
                spans.add(TajwidSpan(i, end, TajwidType.MAD_WAJIB_JAIZ, text.substring(i, end)))
                i = end
                continue
            }

            // 4. Small Meem Iqlab marker (ۢ or ۭ)
            if (c == SMALL_HIGH_MEEM || c == SMALL_LOW_MEEM) {
                val start = if (i > 0 && isArabicLetter(text[i - 1])) i - 1 else i
                val nextLetterIdx = findNextArabicLetter(text, i + 1)
                val end = if (nextLetterIdx != -1) findDiacriticEnd(text, nextLetterIdx + 1) else i + 1
                spans.add(TajwidSpan(start, end, TajwidType.IQLAB, text.substring(start, end)))
                i = if (nextLetterIdx != -1) end else i + 1
                continue
            }

            if (isArabicLetter(c)) {
                val diacriticEnd = findDiacriticEnd(text, i + 1)
                val diacritics = text.substring(i + 1, diacriticEnd)

                // ── Rule 1: Ghunnah Musyaddadah ── (Nun/Mim with Shadda: نّ or مّ)
                if ((c == NUN || c == MIM) && diacritics.contains(SHADDA)) {
                    spans.add(TajwidSpan(i, diacriticEnd, TajwidType.GHUNNAH, text.substring(i, diacriticEnd)))
                    i = diacriticEnd
                    continue
                }

                // ── Rule 2: Qalqalah ── (ق ط ب ج د) with Sukun
                if (c in QALQALAH_LETTERS && (diacritics.contains(SUKUN) || diacritics.contains(QURANIC_SUKUN))) {
                    spans.add(TajwidSpan(i, diacriticEnd, TajwidType.QALQALAH, text.substring(i, diacriticEnd)))
                    i = diacriticEnd
                    continue
                }

                // ── Rule 3: Mim Sukun Rules (Idgham Mimi & Ikhfa Syafawi) ──
                val isMimSukun = c == MIM && (diacritics.contains(SUKUN) || diacritics.contains(QURANIC_SUKUN) || (diacritics.isEmpty() && i + 1 < len && text[i + 1] == ' '))
                if (isMimSukun) {
                    val nextLetterIdx = findNextArabicLetter(text, diacriticEnd)
                    if (nextLetterIdx != -1) {
                        val nextLetter = text[nextLetterIdx]
                        val nextDiacriticEnd = findDiacriticEnd(text, nextLetterIdx + 1)
                        if (nextLetter == MIM) {
                            // Idgham Mitslain / Mimi -> Color BOTH Mims
                            spans.add(TajwidSpan(i, nextDiacriticEnd, TajwidType.IDGHAM_MIM_MIMI, text.substring(i, nextDiacriticEnd)))
                            i = nextDiacriticEnd
                            continue
                        } else if (nextLetter == BA) {
                            // Ikhfa Syafawi -> Color BOTH Mim and Ba
                            spans.add(TajwidSpan(i, nextDiacriticEnd, TajwidType.IKHFA_SYAFAWI, text.substring(i, nextDiacriticEnd)))
                            i = nextDiacriticEnd
                            continue
                        }
                    }
                }

                // ── Rule 4: Nun Sukun / Tanwin Rules (Inter-Character Two-Letter Pairing) ──
                val isNunSukun = c == NUN && (diacritics.contains(SUKUN) || diacritics.contains(QURANIC_SUKUN) || (diacritics.isEmpty() && i + 1 < len && text[i + 1] == ' '))
                val hasTanwin = diacritics.any { it == FATHATAN || it == DAMMATAN || it == KASRATAN }

                if (isNunSukun || hasTanwin) {
                    val nextLetterIdx = findNextArabicLetter(text, diacriticEnd)
                    if (nextLetterIdx != -1) {
                        val nextLetter = text[nextLetterIdx]
                        val nextDiacriticEnd = findDiacriticEnd(text, nextLetterIdx + 1)

                        when {
                            // Iqlab: Nun/Tanwin + Ba -> Color BOTH
                            nextLetter == BA -> {
                                spans.add(TajwidSpan(i, nextDiacriticEnd, TajwidType.IQLAB, text.substring(i, nextDiacriticEnd)))
                                i = nextDiacriticEnd
                                continue
                            }
                            // Idgham Bighunnah: Nun/Tanwin + (ي ن م و) -> Color BOTH
                            nextLetter in IDGHAM_BIGHUNNAH_LETTERS -> {
                                spans.add(TajwidSpan(i, nextDiacriticEnd, TajwidType.IDGHAM_BIGHUNNAH, text.substring(i, nextDiacriticEnd)))
                                i = nextDiacriticEnd
                                continue
                            }
                            // Idgham Bilaghunnah: Nun/Tanwin + (ل ر) -> Color BOTH
                            nextLetter in IDGHAM_BILAGHUNNAH_LETTERS -> {
                                spans.add(TajwidSpan(i, nextDiacriticEnd, TajwidType.IDGHAM_BILAGHUNNAH, text.substring(i, nextDiacriticEnd)))
                                i = nextDiacriticEnd
                                continue
                            }
                            // Ikhfa Haqiqi: Nun/Tanwin + 15 Ikhfa letters -> Color BOTH
                            nextLetter in IKHFA_LETTERS -> {
                                spans.add(TajwidSpan(i, nextDiacriticEnd, TajwidType.IKHFA_HAQIQI, text.substring(i, nextDiacriticEnd)))
                                i = nextDiacriticEnd
                                continue
                            }
                        }
                    }
                }

                // ── Rule 5: Natural Mad (Alif after Fatha, Waw after Damma, Ya after Kasra) ──
                val prevLetterIdx = findPrevArabicLetter(text, i)
                if (prevLetterIdx != -1) {
                    val prevDiacritics = text.substring(prevLetterIdx + 1, i)
                    val isAlifMad = (c == ALIF || c == ALIF_MAQSURA) && prevDiacritics.contains(FATHA) && !diacritics.contains(SHADDA) && !diacritics.contains(SUKUN)
                    val isWawMad = c == WAW && prevDiacritics.contains(DAMMA) && (diacritics.contains(SUKUN) || diacritics.contains(QURANIC_SUKUN) || diacritics.isEmpty())
                    val isYaMad = (c == YA || c == ALIF_MAQSURA) && prevDiacritics.contains(KASRA) && (diacritics.contains(SUKUN) || diacritics.contains(QURANIC_SUKUN) || diacritics.isEmpty())

                    if (isAlifMad || isWawMad || isYaMad) {
                        spans.add(TajwidSpan(prevLetterIdx, diacriticEnd, TajwidType.MAD_TABII, text.substring(prevLetterIdx, diacriticEnd)))
                        i = diacriticEnd
                        continue
                    }
                }

                i = diacriticEnd
                continue
            }

            i++
        }

        return spans
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
            // Stop at verse marker / punctuation
            if (c == '\n' || c == 'ۖ' || c == 'ۗ' || c == 'ۚ' || c == 'ۛ' || c == 'ۜ' || c == '۝' || c == 'ۘ' || c == 'ۙ') return -1
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

