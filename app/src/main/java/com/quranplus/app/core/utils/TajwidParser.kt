package com.quranplus.app.core.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.quranplus.app.core.ui.theme.QuranColors
import java.util.Locale

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
        ),
        HAMZAT_WASL(
            label = "Hamzat Wasl",
            color = QuranColors.TajwidIzhar,
            harakatDuration = "Sesuai posisi mulai",
            description = "Hamzah yang dibaca saat memulai bacaan",
            ruleExplanation = "Hamzat wasl dibaca ketika memulai kata dan gugur ketika bacaan disambung dari kata sebelumnya."
        ),
        SILENT(
            label = "Huruf Saktah",
            color = QuranColors.TajwidIzhar,
            harakatDuration = "Tidak dibaca",
            description = "Tanda huruf yang tidak dilafalkan",
            ruleExplanation = "Tanda silent pada mushaf menandai huruf yang tidak dilafalkan dalam bacaan."
        ),
        LAM_SHAMSIYYAH(
            label = "Lam Syamsiyyah",
            color = QuranColors.TajwidIdgham,
            harakatDuration = "Melebur",
            description = "Lam ta'rif melebur ke huruf syamsiyyah",
            ruleExplanation = "Lam pada alif-lam ta'rif tidak terdengar dan melebur ke huruf syamsiyyah setelahnya."
        ),
        MAD_PERMISSIBLE(
            label = "Mad Jaiz",
            color = QuranColors.TajwidMadWajib,
            harakatDuration = "2, 4, atau 6 Harakat",
            description = "Panjang bacaan yang diperbolehkan",
            ruleExplanation = "Mad permissible ditandai oleh sumber Tajwid dan dibaca sesuai riwayat serta pedoman bacaan yang dipilih."
        ),
        IDGHAM_MUTAJANISAIN(
            label = "Idgham Mutajanisain",
            color = QuranColors.TajwidIdgham,
            harakatDuration = "Melebur",
            description = "Dua huruf yang makhrajnya sama",
            ruleExplanation = "Huruf pertama dilebur ke huruf kedua ketika dua huruf yang satu makhraj bertemu sesuai tanda Tajwid."
        ),
        IDGHAM_MUTAQARIBAIN(
            label = "Idgham Mutaqaribain",
            color = QuranColors.TajwidIdghamBila,
            harakatDuration = "Melebur",
            description = "Dua huruf yang makhrajnya berdekatan",
            ruleExplanation = "Huruf pertama dilebur ke huruf kedua ketika dua huruf yang berdekatan makhrajnya bertemu sesuai tanda Tajwid."
        )

        ;

        companion object {
            /**
             * Maps the compact tag IDs stored by the Quran Tajwid edition to a typed rule.
             * The IDs are source data, not inferred from the displayed Arabic text.
             */
            fun fromSourceTag(tag: String): TajwidType? = when (TajwidTagCatalog.ruleIdFor(tag)) {
                TajwidRuleId.HAMZAT_WASL -> HAMZAT_WASL
                TajwidRuleId.SILENT -> SILENT
                TajwidRuleId.LAM_SHAMSIYYAH -> LAM_SHAMSIYYAH
                TajwidRuleId.MAD_TABII -> MAD_TABII
                TajwidRuleId.MAD_PERMISSIBLE -> MAD_PERMISSIBLE
                TajwidRuleId.MAD_LAZIM -> MAD_LAZIM
                TajwidRuleId.QALQALAH -> QALQALAH
                TajwidRuleId.MAD_WAJIB_JAIZ -> MAD_WAJIB_JAIZ
                TajwidRuleId.IKHFA_SYAFAWI -> IKHFA_SYAFAWI
                TajwidRuleId.IKHFA_HAQIQI -> IKHFA_HAQIQI
                TajwidRuleId.IDGHAM_MIM_MIMI -> IDGHAM_MIM_MIMI
                TajwidRuleId.IQLAB -> IQLAB
                TajwidRuleId.IDGHAM_BIGHUNNAH -> IDGHAM_BIGHUNNAH
                TajwidRuleId.IDGHAM_BILAGHUNNAH -> IDGHAM_BILAGHUNNAH
                TajwidRuleId.IDGHAM_MUTAJANISAIN -> IDGHAM_MUTAJANISAIN
                TajwidRuleId.IDGHAM_MUTAQARIBAIN -> IDGHAM_MUTAQARIBAIN
                TajwidRuleId.GHUNNAH -> GHUNNAH
                else -> null
            }
        }
    }

    const val TAJWID_ANNOTATION = "quranplus_tajwid"
    const val TAJWID_SOURCE_ANNOTATION = "quranplus_tajwid_source"

    /**
     * Result of parsing the bracket syntax used by the bundled Quran database.
     * Unknown tags are retained in [unknownTags] and rendered without a guessed rule.
     */
    data class TaggedTextResult(
        val text: String,
        val spans: List<TajwidSpan>,
        val unknownTags: Set<String>,
        val malformed: Boolean
    )

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
        val snippet: String = "",
        val sourceTag: String? = null
    )

    private val bracketTagPattern = Regex("\\[([a-zA-Z])(?::([0-9]+))?\\[")


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

        if (!tajwidTags.isNullOrBlank() && bracketTagPattern.containsMatchIn(tajwidTags)) {
            val parsed = parseBracketTags(tajwidTags)
            val alignedSpans = alignSpansToDisplay(parsed, arabicText)
            if (!parsed.malformed && alignedSpans != null) {
                return buildAnnotatedText(
                    text = arabicText,
                    spans = alignedSpans,
                    baseTextColor = baseTextColor
                )
            }

            // The source markup and display text must share the same codepoint offsets.
            // Returning an uncoloured text is safer than applying a wrong rule to a glyph.
            return plainText(arabicText, baseTextColor)
        }

        // If text contains inline tags like <tajwid:idgham>...
        if (arabicText.contains("<tajwid:") || (!tajwidTags.isNullOrBlank() && arabicText.contains("<"))) {
            return parseTaggedArabic(arabicText, baseTextColor)
        }

        // Tajwid is only trustworthy when it comes from an explicit source tag.
        // Do not infer a rule from Arabic glyphs or harakat in the display text.
        return plainText(arabicText, baseTextColor)
    }

    /**
     * Extracts all detected Tajwid occurrences with their full details for bottom sheet display.
     */
    fun extractTajwidOccurrences(arabicText: String, tajwidTags: String? = null): List<TajwidSpan> {
        if (arabicText.isBlank()) return emptyList()
        if (!tajwidTags.isNullOrBlank() && bracketTagPattern.containsMatchIn(tajwidTags)) {
            val parsed = parseBracketTags(tajwidTags)
            return alignSpansToDisplay(parsed, arabicText).orEmpty()
        }
        // A missing tag column is an unavailable source, not permission to infer
        // a rule from the rendered Arabic glyphs.
        return emptyList()
    }

    /**
     * Parses the `[rule[:source-id][content]` format used by the bundled edition.
     * The source id is retained as an annotation so a future detail screen can link
     * back to the exact source occurrence without deriving data from the glyph.
     */
    fun parseBracketTags(text: String): TaggedTextResult {
        val plainText = StringBuilder()
        val spans = mutableListOf<TajwidSpan>()
        val unknownTags = linkedSetOf<String>()
        data class OpenTag(
            val tag: String,
            val sourceId: String,
            val start: Int
        )

        val openTags = ArrayDeque<OpenTag>()
        var cursor = 0
        var malformed = false

        while (cursor < text.length) {
            val match = bracketTagPattern.find(text, cursor)
            if (match != null && match.range.first == cursor) {
                openTags.addLast(
                    OpenTag(
                        tag = match.groupValues[1].lowercase(Locale.ROOT),
                        sourceId = match.groupValues.getOrNull(2).orEmpty(),
                        start = plainText.length
                    )
                )
                cursor = match.range.last + 1
                continue
            }

            if (text[cursor] == '[') {
                val literalEnd = text.indexOf(']', startIndex = cursor + 1)
                val literal = if (literalEnd > cursor) text.substring(cursor + 1, literalEnd) else null
                if (literal == "ٮٰ") {
                    plainText.append(normalizeSourceContent(literal))
                    cursor = literalEnd + 1
                    continue
                }
            }

            when {
                text[cursor] == ']' -> {
                    val openTag = openTags.removeLastOrNull()
                    if (openTag == null) {
                        malformed = true
                    } else {
                        val type = TajwidType.fromSourceTag(openTag.tag)
                        if (type == null) {
                            unknownTags += openTag.tag
                        } else if (openTag.start < plainText.length) {
                            spans += TajwidSpan(
                                start = openTag.start,
                                end = plainText.length,
                                type = type,
                                snippet = plainText.substring(openTag.start),
                                sourceTag = if (openTag.sourceId.isEmpty()) {
                                    openTag.tag
                                } else {
                                    "${openTag.tag}:${openTag.sourceId}"
                                }
                            )
                        }
                    }
                    cursor++
                }
                else -> {
                    plainText.append(normalizeSourceContent(text[cursor].toString()))
                    cursor++
                }
            }
        }

        if (openTags.isNotEmpty()) malformed = true

        return TaggedTextResult(
            text = plainText.toString(),
            spans = spans,
            unknownTags = unknownTags,
            malformed = malformed
        )
    }

    private fun normalizeSourceContent(content: String): String {
        return buildString(content.length) {
            content.forEach { char ->
                when (char) {
                    '\u0640', '\u200C' -> Unit
                    '\u0672' -> append('\u0670')
                    '\u066E' -> append('\u0649')
                    '\u06E7' -> append('\u06E6')
                    else -> append(char)
                }
            }
        }
    }

    private fun buildAnnotatedText(
        text: String,
        spans: List<TajwidSpan>,
        baseTextColor: Color
    ): AnnotatedString {
        val builder = AnnotatedString.Builder(text)
        if (text.isNotEmpty()) {
            builder.addStyle(SpanStyle(color = baseTextColor), 0, text.length)
        }

        spans.forEach { span ->
            if (span.start in 0 until text.length && span.end in (span.start + 1)..text.length) {
                builder.addStyle(SpanStyle(color = span.type.color), span.start, span.end)
                builder.addStringAnnotation(TAJWID_ANNOTATION, span.type.name, span.start, span.end)
                span.sourceTag?.let { sourceTag ->
                    builder.addStringAnnotation(TAJWID_SOURCE_ANNOTATION, sourceTag, span.start, span.end)
                }
            }
        }
        return builder.toAnnotatedString()
    }

    private fun plainText(text: String, baseTextColor: Color): AnnotatedString {
        return if (text.isEmpty()) AnnotatedString(text) else buildAnnotatedString {
            withStyle(SpanStyle(color = baseTextColor)) { append(text) }
        }
    }

    private fun alignSpansToDisplay(
        parsed: TaggedTextResult,
        displayText: String
    ): List<TajwidSpan>? {
        if (displayText.startsWith(parsed.text)) {
            return parsed.spans.map { span ->
                span.copy(snippet = displayText.substring(span.start, span.end))
            }
        }
        if (displayText.startsWith(BISMILLAH_PREFIX)) {
            val prefixLength = BISMILLAH_PREFIX.length
            val aligned = alignSpansToDisplay(parsed, displayText.substring(prefixLength))
                ?: return null
            return aligned.map { span ->
                span.copy(
                    start = span.start + prefixLength,
                    end = span.end + prefixLength,
                    snippet = displayText.substring(span.start + prefixLength, span.end + prefixLength)
                )
            }
        }
        val sourceToDisplay = alignSourceToDisplay(parsed.text, displayText) ?: return null
        return parsed.spans.mapNotNull { span ->
            val start = sourceToDisplay[span.start.coerceIn(0, parsed.text.length)]
            val end = sourceToDisplay[span.end.coerceIn(0, parsed.text.length)]
            if (end <= start) return@mapNotNull null
            span.copy(
                start = start,
                end = end,
                snippet = displayText.substring(start, end)
            )
        }
    }

    /**
     * Aligns the markup edition with the display edition without rewriting the
     * Quran text. The two editions contain known decorative/codepoint variants;
     * only low-cost equivalents are accepted, otherwise coloring is blocked.
     */
    private fun alignSourceToDisplay(source: String, display: String): IntArray? {
        val sourceLength = source.length
        val displayLength = display.length
        val width = displayLength + 1
        val costs = IntArray((sourceLength + 1) * width) { Int.MAX_VALUE / 4 }
        val operations = ByteArray(costs.size)
        fun index(sourceIndex: Int, displayIndex: Int): Int = sourceIndex * width + displayIndex

        costs[index(sourceLength, displayLength)] = 0
        for (sourceIndex in sourceLength downTo 0) {
            for (displayIndex in displayLength downTo 0) {
                if (sourceIndex == sourceLength && displayIndex == displayLength) continue
                var bestCost = Int.MAX_VALUE / 4
                var bestOperation = OPERATION_NONE

                if (sourceIndex < sourceLength && displayIndex < displayLength) {
                    val equivalent = areSourceAndDisplayEquivalent(
                        source[sourceIndex],
                        display[displayIndex]
                    )
                    if (equivalent) {
                        val candidate = costs[index(sourceIndex + 1, displayIndex + 1)]
                        if (candidate < bestCost) {
                            bestCost = candidate
                            bestOperation = OPERATION_MATCH
                        }
                    }
                }

                if (sourceIndex < sourceLength && isOptionalSourceCharacter(
                        source,
                        sourceIndex,
                        display,
                        displayIndex
                    )
                ) {
                    val candidate = costs[index(sourceIndex + 1, displayIndex)]
                    if (candidate < bestCost) {
                        bestCost = candidate
                        bestOperation = OPERATION_DELETE_SOURCE
                    }
                }

                if (displayIndex < displayLength && isOptionalDisplayCharacter(
                        source,
                        sourceIndex,
                        display,
                        displayIndex
                    )
                ) {
                    val candidate = costs[index(sourceIndex, displayIndex + 1)]
                    if (candidate < bestCost) {
                        bestCost = candidate
                        bestOperation = OPERATION_INSERT_DISPLAY
                    }
                }

                costs[index(sourceIndex, displayIndex)] = bestCost
                operations[index(sourceIndex, displayIndex)] = bestOperation
            }
        }

        val totalCost = costs[index(0, 0)]
        if (totalCost > maxOf(MAX_ALIGNMENT_COST, sourceLength / 3)) return null

        val sourceBoundaries = IntArray(sourceLength + 1)
        var sourceIndex = 0
        var displayIndex = 0
        sourceBoundaries[0] = 0
        while (sourceIndex < sourceLength || displayIndex < displayLength) {
            when (operations[index(sourceIndex, displayIndex)]) {
                OPERATION_MATCH -> {
                    sourceIndex++
                    displayIndex++
                    sourceBoundaries[sourceIndex] = displayIndex
                }
                OPERATION_DELETE_SOURCE -> {
                    sourceIndex++
                    sourceBoundaries[sourceIndex] = displayIndex
                }
                OPERATION_INSERT_DISPLAY -> {
                    displayIndex++
                    sourceBoundaries[sourceIndex] = displayIndex
                }
                else -> return null
            }
        }
        return sourceBoundaries
    }

    private fun areSourceAndDisplayEquivalent(source: Char, display: Char): Boolean {
        if (source == display) return true
        return (source == '\u0652' && display == '\u06DF') ||
            (source == '\u0623' && display == '\u0621') ||
            (source == '\u0649' && display == '\u0626') ||
            (source == '\u0648' && display == '\u0624')
    }

    private fun isOptionalSourceCharacter(
        source: String,
        sourceIndex: Int,
        display: String,
        displayIndex: Int
    ): Boolean {
        val char = source[sourceIndex]
        if (char == ' ' || char == '\u200C' || char in OPTIONAL_ALIGNMENT_MARKS) return true
        return char == ALIF &&
            sourceIndex > 0 &&
            displayIndex > 0 &&
            source[sourceIndex - 1] == HAMZA &&
            display[displayIndex - 1] == ALIF_HAMZA
    }

    private fun isOptionalDisplayCharacter(
        source: String,
        sourceIndex: Int,
        display: String,
        displayIndex: Int
    ): Boolean {
        val char = display[displayIndex]
        if (char == ' ' || char in DISPLAY_ONLY_MARKS || char in OPTIONAL_ALIGNMENT_MARKS) return true
        return char == ALIF &&
            sourceIndex > 0 &&
            displayIndex > 0 &&
            source[sourceIndex - 1] == ALIF_HAMZA &&
            display[displayIndex - 1] == HAMZA
    }

    private const val OPERATION_NONE: Byte = 0
    private const val OPERATION_MATCH: Byte = 1
    private const val OPERATION_DELETE_SOURCE: Byte = 2
    private const val OPERATION_INSERT_DISPLAY: Byte = 3
    private const val MAX_ALIGNMENT_COST = 24
    private const val BISMILLAH_PREFIX = "بِّسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ "
    private const val HAMZA = '\u0621'
    private const val ALIF_HAMZA = '\u0623'
    private val DISPLAY_ONLY_MARKS = setOf(
        '\u06DF', '\u06E0', '\u06E2', '\u06ED', '\u200C',
        WaqafParser.WAQAF_LA_SYM.single(), WaqafParser.WAQAF_JAIZ_SYM.single(),
        WaqafParser.WAQAF_WASHLA_SYM.single(), WaqafParser.WAQAF_AWLA_SYM.single(),
        WaqafParser.WAQAF_MUANAQAH_SYM.single(), WaqafParser.WAQAF_SAKTAH_SYM.single(),
        WaqafParser.WAQAF_LAZIM_SYM.single(), WaqafParser.AYAH_END_SYM.single()
    ) + ('٠'..'٩').toSet()
    private val OPTIONAL_ALIGNMENT_MARKS = setOf(
        '\u064B', '\u064C', '\u064D', '\u064E', '\u064F', '\u0650', '\u0651', '\u0652',
        '\u0670', '\u0649', '\u06E5', '\u06E6'
    )

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

}
