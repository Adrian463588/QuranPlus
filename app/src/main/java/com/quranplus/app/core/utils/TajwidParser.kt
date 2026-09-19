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
        val color: Color?,
        val harakatDuration: String,
        val description: String,
        val ruleExplanation: String,
        val exampleArabic: String = "",
        val exampleLatin: String = ""
    ) {
        GHUNNAH(
            label = "Ghunnah Musyaddadah",
            color = QuranColors.TajwidGhunnah,
            harakatDuration = "2-3 Harakat",
            description = "Nun & Mim bertasydid",
            ruleExplanation = "Nun atau Mim bertasydid dibaca dengan dengung sempurna yang ditahan selama 2-3 harakat.",
            exampleArabic = "ثُمَّ لَتَرَوُنَّهَا",
            exampleLatin = "Nun / Mim bertasydid"
        ),
        IDGHAM_BIGHUNNAH(
            label = "Idgham Bighunnah",
            color = QuranColors.TajwidIdgham,
            harakatDuration = "2 Harakat",
            description = "Nun mati/tanwin bertemu ي ن م و",
            ruleExplanation = "Nun mati atau tanwin melebur ke huruf berikutnya disertai dengung selama 2 harakat.",
            exampleArabic = "فَمَن يَّعْمَلْ",
            exampleLatin = "Nun mati bertemu Ya"
        ),
        IDGHAM_BILAGHUNNAH(
            label = "Idgham Bilaghunnah",
            color = QuranColors.TajwidIdghamBila,
            harakatDuration = "Tanpa Dengung",
            description = "Nun mati/tanwin bertemu ل ر",
            ruleExplanation = "Nun mati atau tanwin melebur sempurna ke dalam Lam atau Ra tanpa dengung.",
            exampleArabic = "وَيْلٌ لِّكُلِّ",
            exampleLatin = "Tanwin bertemu Lam"
        ),
        IDGHAM_MIM_MIMI(
            label = "Idgham Mitslain / Mimi",
            color = QuranColors.TajwidIdghamMimi,
            harakatDuration = "2 Harakat",
            description = "Mim mati bertemu Mim",
            ruleExplanation = "Mim mati melebur ke dalam Mim berharakat berikutnya disertai dengung 2 harakat.",
            exampleArabic = "لَهُم مَّا كَانُوا",
            exampleLatin = "Mim mati bertemu Mim"
        ),
        IQLAB(
            label = "Iqlab",
            color = QuranColors.TajwidIqlab,
            harakatDuration = "2 Harakat",
            description = "Nun mati/tanwin bertemu ب",
            ruleExplanation = "Bunyi Nun mati atau tanwin diganti menjadi bunyi Mim samar disertai dengung 2 harakat sebelum melafalkan Ba.",
            exampleArabic = "مِنۢ بَعْدِ",
            exampleLatin = "Nun mati bertemu Ba"
        ),
        IKHFA_HAQIQI(
            label = "Ikhfa Haqiqi",
            color = QuranColors.TajwidIkhfa,
            harakatDuration = "2 Harakat",
            description = "Nun mati/tanwin bertemu 15 huruf ikhfa",
            ruleExplanation = "Nun mati atau tanwin disamarkan antara Izhar dan Idgham dengan dengung 2 harakat.",
            exampleArabic = "مِن قَبْلُ",
            exampleLatin = "Nun mati bertemu Qaf"
        ),
        IKHFA_SYAFAWI(
            label = "Ikhfa Syafawi",
            color = QuranColors.TajwidIkhfaSyafawi,
            harakatDuration = "2 Harakat",
            description = "Mim mati bertemu ب",
            ruleExplanation = "Mim mati disamarkan di kedua bibir disertai dengung 2 harakat saat bertemu huruf Ba.",
            exampleArabic = "تَرْمِيهِم بِحِجَارَةٍ",
            exampleLatin = "Mim mati bertemu Ba"
        ),
        QALQALAH(
            label = "Qalqalah",
            color = QuranColors.TajwidQalqalah,
            harakatDuration = "Pantulan (Sughra/Kubra)",
            description = "Huruf ق ط ب ج د sukun / waqaf",
            ruleExplanation = "Huruf Qalqalah dipantulkan bunyinya saat berharakat sukun di tengah (Sughra) atau saat berhenti di akhir kata/ayat (Kubra).",
            exampleArabic = "اقْرَأْ بِاسْمِ رَبِّكَ",
            exampleLatin = "Qaf sukun di awal/tengah"
        ),
        IZHAR_HALQI(
            label = "Izhar Halqi / Syafawi",
            color = null,
            harakatDuration = "Jelas (Tanpa Dengung)",
            description = "Nun/Mim mati dibaca jelas",
            ruleExplanation = "Dibaca jelas, tegas, tanpa menambah dengung atau menahan suara.",
            exampleArabic = "مِنْ خَوْفٍ",
            exampleLatin = "Nun mati bertemu Kha"
        ),
        MAD_TABII(
            label = "Mad Tabi'i / Asli",
            color = null,
            harakatDuration = "2 Harakat",
            description = "Panjang 2 harakat",
            ruleExplanation = "Mad asli dengan memanjangkan suara sepanjang 2 harakat (1 alif) secara stabil.",
            exampleArabic = "قَالَ",
            exampleLatin = "Huruf mad asli"
        ),
        MAD_WAJIB_JAIZ(
            label = "Mad Wajib / Jaiz",
            color = QuranColors.TajwidMadWajib,
            harakatDuration = "4-5 Harakat",
            description = "Panjang 4-5 harakat (tanda bendera ~)",
            ruleExplanation = "Mad bertemu hamzah dalam satu kata (Wajib Muttashil) atau kata terpisah (Jaiz Munfashil).",
            exampleArabic = "جَآءَ",
            exampleLatin = "Mad bertemu Hamzah (~)"
        ),
        MAD_LAZIM(
            label = "Mad Lazim / Farq",
            color = QuranColors.TajwidMadLazim,
            harakatDuration = "6 Harakat (Wajib)",
            description = "Panjang 6 harakat wajib",
            ruleExplanation = "Mad bertemu huruf bertasydid atau sukun lazim, wajib dipanjangkan 6 harakat penuh.",
            exampleArabic = "الضَّآلِّينَ",
            exampleLatin = "Mad bertemu Tasydid"
        ),
        HAMZAT_WASL(
            label = "Hamzat Wasl",
            color = null,
            harakatDuration = "Sesuai posisi mulai",
            description = "Hamzah yang dibaca saat memulai bacaan",
            ruleExplanation = "Hamzat wasl dibaca ketika memulai kata dan gugur ketika bacaan disambung dari kata sebelumnya.",
            exampleArabic = "ٱلْحَمْدُ",
            exampleLatin = "Alif washal"
        ),
        SILENT(
            label = "Huruf Saktah / Silent",
            color = null,
            harakatDuration = "Tidak dibaca",
            description = "Tanda huruf yang tidak dilafalkan",
            ruleExplanation = "Tanda silent pada mushaf menandai huruf yang tidak dilafalkan dalam bacaan.",
            exampleArabic = "عَمِلُوا۟",
            exampleLatin = "Alif pelindung silent"
        ),
        LAM_SHAMSIYYAH(
            label = "Lam Syamsiyyah",
            color = null,
            harakatDuration = "Melebur",
            description = "Lam ta'rif melebur ke huruf syamsiyyah",
            ruleExplanation = "Lam pada alif-lam ta'rif tidak terdengar dan melebur ke huruf syamsiyyah setelahnya.",
            exampleArabic = "ٱلرَّحْمَٰنِ",
            exampleLatin = "Lam ta'rif idgham syamsiyyah"
        ),
        MAD_PERMISSIBLE(
            label = "Mad 'Aridh Lissukun",
            color = QuranColors.TajwidMad,
            harakatDuration = "2, 4, atau 6 Harakat",
            description = "Panjang bacaan saat waqaf di akhir ayat",
            ruleExplanation = "Mad bertemu huruf sukun aridh karena berhenti pada waqaf atau akhir ayat.",
            exampleArabic = "نَسْتَعِينُ",
            exampleLatin = "Waqaf di akhir kata"
        ),
        IDGHAM_MUTAJANISAIN(
            label = "Idgham Mutajanisain",
            color = QuranColors.TajwidIdghamMutajanisain,
            harakatDuration = "Melebur",
            description = "Dua huruf yang makhrajnya sama",
            ruleExplanation = "Huruf pertama dilebur ke huruf kedua ketika dua huruf yang satu makhraj bertemu sesuai tanda Tajwid.",
            exampleArabic = "أَثْقَلَت دَّعَوَا",
            exampleLatin = "Ta sukun bertemu Dal"
        ),
        IDGHAM_MUTAQARIBAIN(
            label = "Idgham Mutaqaribain",
            color = QuranColors.TajwidIdghamMutaqaribain,
            harakatDuration = "Melebur",
            description = "Dua huruf yang makhrajnya berdekatan",
            ruleExplanation = "Huruf pertama dilebur ke huruf kedua ketika dua huruf yang berdekatan makhrajnya bertemu sesuai tanda Tajwid.",
            exampleArabic = "أَلَمْ نَخْلُقكُّم",
            exampleLatin = "Qaf sukun bertemu Kaf"
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

    private val INTER_WORD_RULES = setOf(
        TajwidType.IDGHAM_BIGHUNNAH,
        TajwidType.IDGHAM_BILAGHUNNAH,
        TajwidType.IDGHAM_MUTAJANISAIN,
        TajwidType.IDGHAM_MUTAQARIBAIN,
        TajwidType.IQLAB,
        TajwidType.IKHFA_HAQIQI
    )

    private val WAQAF_MARKS = setOf(
        '\u06D6', '\u06D7', '\u06D8', '\u06D9', '\u06DA', '\u06DB', '\u06DC', '\u06E9',
        'ۖ', 'ۗ', 'ۚ', 'ۘ', 'ۙ', 'ۜ', 'ۛ', '۝', 'ࣖ'
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
            if (!parsed.malformed && parsed.unknownTags.isEmpty() && alignedSpans != null) {
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
            if (parsed.malformed || parsed.unknownTags.isNotEmpty()) return emptyList()
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
                            val snippet = plainText.substring(openTag.start)
                            val containsWaqaf = snippet.any { it in WAQAF_MARKS }
                            val isInterWordRule = type in INTER_WORD_RULES
                            if (!(isInterWordRule && containsWaqaf)) {
                                spans += TajwidSpan(
                                    start = openTag.start,
                                    end = plainText.length,
                                    type = type,
                                    snippet = snippet,
                                    sourceTag = if (openTag.sourceId.isEmpty()) {
                                        openTag.tag
                                    } else {
                                        "${openTag.tag}:${openTag.sourceId}"
                                    }
                                )
                            }
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
            val color = span.type.color
            if (color != null && span.start in 0 until text.length && span.end in (span.start + 1)..text.length) {
                builder.addStyle(SpanStyle(color = color), span.start, span.end)
            }
            if (span.start in 0 until text.length && span.end in (span.start + 1)..text.length) {
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

    private const val BISMILLAH_PREFIX = "بِّسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ "

    private val IGNORABLE_ALIGNMENT_MARKS: Set<Char> = setOf(
        '\u0640', '\u200C', '\u200D', '\u200B', '\uFEFF', '\u00A0',
        // Waqaf symbols
        WaqafParser.WAQAF_LA_SYM.single(), WaqafParser.WAQAF_JAIZ_SYM.single(),
        WaqafParser.WAQAF_WASHLA_SYM.single(), WaqafParser.WAQAF_AWLA_SYM.single(),
        WaqafParser.WAQAF_MUANAQAH_SYM.single(), WaqafParser.WAQAF_SAKTAH_SYM.single(),
        WaqafParser.WAQAF_LAZIM_SYM.single(), WaqafParser.AYAH_END_SYM.single(),
        // Small Quranic signs & extra diacritics
        '\u06DF', '\u06E0', '\u06E1', '\u06E2', '\u06E3', '\u06E4', '\u06E5', '\u06E6', '\u06E7', '\u06E8', '\u06EA', '\u06EB', '\u06EC', '\u06ED',
        '\u0653', '\u0654', '\u0655', '\u0656', '\u0657', '\u0658', '\u065C', '\u0670',
        // Harakat
        '\u064B', '\u064C', '\u064D', '\u064E', '\u064F', '\u0650', '\u0651', '\u0652'
    ) + ('٠'..'٩').toSet() + ('0'..'9').toSet()

    private fun normalizeBaseChar(c: Char): Char = when (c) {
        '\u0621', '\u0622', '\u0623', '\u0625', '\u0671', '\u0627' -> 'ا'
        '\u0649', '\u064A', '\u0626', '\u06CC', '\u066E' -> 'ي'
        '\u0624', '\u0648', '\u06E5' -> 'و'
        '\u0652', '\u06DF', '\u06E1' -> '\u0652'
        '\u0670', '\u0672' -> '\u0670'
        else -> c
    }

    /**
     * Aligns the markup edition with the display edition across all Uthmani codepoint
     * variants (tatweel, waqaf marks, hamza/alif orthography, and vowel marks).
     */
    private fun alignSourceToDisplay(source: String, display: String): IntArray? {
        val sLen = source.length
        val dLen = display.length
        var sIdx = 0
        var dIdx = 0
        val mapping = IntArray(sLen + 1)

        if (display.startsWith(BISMILLAH_PREFIX) && !source.startsWith("بِسْمِ")) {
            dIdx = BISMILLAH_PREFIX.length
        }

        while (sIdx < sLen && dIdx < dLen) {
            val sChar = source[sIdx]
            val dChar = display[dIdx]

            if (sChar == dChar || normalizeBaseChar(sChar) == normalizeBaseChar(dChar)) {
                sIdx++
                dIdx++
                mapping[sIdx] = dIdx
            } else if (dChar == ' ' || dChar in IGNORABLE_ALIGNMENT_MARKS) {
                dIdx++
            } else if (sChar == ' ' || sChar in IGNORABLE_ALIGNMENT_MARKS) {
                sIdx++
                mapping[sIdx] = dIdx
            } else if (normalizeBaseChar(dChar) == 'ا' && sIdx > 0 && normalizeBaseChar(source[sIdx - 1]) == 'ا') {
                dIdx++
            } else if (normalizeBaseChar(sChar) == 'ا' && dIdx > 0 && normalizeBaseChar(display[dIdx - 1]) == 'ا') {
                sIdx++
                mapping[sIdx] = dIdx
            } else if (normalizeBaseChar(dChar) == 'ي' && dIdx + 1 < dLen && display[dIdx + 1] == '\u0670') {
                dIdx++
            } else if (normalizeBaseChar(sChar) == 'ي' && sIdx + 1 < sLen && source[sIdx + 1] == '\u0670') {
                sIdx++
                mapping[sIdx] = dIdx
            } else {
                // Lookahead in display
                var foundD = -1
                for (look in 1..8) {
                    if (dIdx + look < dLen) {
                        val ch = display[dIdx + look]
                        if (ch == sChar || normalizeBaseChar(ch) == normalizeBaseChar(sChar)) {
                            foundD = dIdx + look
                            break
                        }
                    }
                }
                if (foundD != -1) {
                    dIdx = foundD + 1
                    sIdx++
                    mapping[sIdx] = dIdx
                    continue
                }

                // Lookahead in source
                var foundS = -1
                for (look in 1..8) {
                    if (sIdx + look < sLen) {
                        val ch = source[sIdx + look]
                        if (ch == dChar || normalizeBaseChar(ch) == normalizeBaseChar(dChar)) {
                            foundS = sIdx + look
                            break
                        }
                    }
                }
                if (foundS != -1) {
                    for (k in sIdx until foundS) {
                        mapping[k + 1] = dIdx
                    }
                    sIdx = foundS + 1
                    dIdx++
                    mapping[sIdx] = dIdx
                    continue
                }

                return null
            }
        }

        while (sIdx < sLen) {
            val sChar = source[sIdx]
            if (sChar == ' ' || sChar in IGNORABLE_ALIGNMENT_MARKS) {
                sIdx++
                mapping[sIdx] = dIdx
            } else {
                break
            }
        }

        return if (sIdx == sLen) mapping else null
    }

    // ─── Tagged XML parser ───────────────────────────────────────────────────
    private fun parseTaggedArabic(text: String, defaultColor: Color): AnnotatedString {
        val tagRegex = Regex("""<tajwid:([a-zA-Z_]+)>(.*?)</tajwid>""")
        val matches = tagRegex.findAll(text).toList()
        if (matches.isEmpty()) return plainText(stripTaggedArabic(text), defaultColor)

        val resolved = matches.map { match ->
            match to runCatching {
                TajwidType.valueOf(match.groupValues[1].uppercase(Locale.ROOT))
            }.getOrNull()
        }
        if (resolved.any { it.second == null }) {
            return plainText(stripTaggedArabic(text), defaultColor)
        }

        return buildAnnotatedString {
            var index = 0
            resolved.forEach { (match, type) ->
                if (match.range.first > index) {
                    withStyle(SpanStyle(color = defaultColor)) {
                        append(text.substring(index, match.range.first))
                    }
                }
                val color = type!!.color ?: defaultColor
                withStyle(SpanStyle(color = color)) { append(match.groupValues[2]) }
                index = match.range.last + 1
            }
            if (index < text.length) {
                withStyle(SpanStyle(color = defaultColor)) { append(text.substring(index)) }
            }
        }
    }

    private fun stripTaggedArabic(text: String): String =
        text.replace(Regex("</?tajwid:[a-zA-Z_]+>"), "")

}
