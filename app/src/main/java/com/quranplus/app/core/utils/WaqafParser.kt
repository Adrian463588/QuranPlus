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
    const val WAQAF_RUKU_SYM = "ࣖ"
    const val SAJDAH_SYM = "۩"
    const val AYAH_END_SYM = "۝"
    val WAQAF_MARKER_SYMBOLS: Set<Char> = setOf(
        WAQAF_LA_SYM.single(),
        WAQAF_JAIZ_SYM.single(),
        WAQAF_WASHLA_SYM.single(),
        WAQAF_AWLA_SYM.single(),
        WAQAF_MUANAQAH_SYM.single(),
        WAQAF_SAKTAH_SYM.single(),
        WAQAF_LAZIM_SYM.single(),
        'ع',
        WAQAF_RUKU_SYM.single(),
        SAJDAH_SYM.single()
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
    ) {
        val displayGlyph: String
            get() = when (symbol) {
                SAJDAH_SYM -> "۩"
                AYAH_END_SYM -> "۝"
                WAQAF_MUANAQAH_SYM -> "∴"
                else -> arabicName
            }
    }

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
        ),
        WaqafRule(
            symbol = WAQAF_RUKU_SYM,
            arabicName = "ع",
            latinName = "Tanda Ruku' (Akhir Maqra')",
            meaning = "Tanda selesainya satu tema bahasan atau ruku'.",
            recommendation = "Dianjurkan berhenti di sini terutama saat membaca dalam shalat.",
            actionCategory = ActionCategory.PREFERRED_STOP,
            badgeColor = QuranColors.Secondary,
            detailedRule = "Menandai selesainya satu kesatuan tema atau ruku' dalam mushaf Al-Qur'an.",
            exampleAyah = "ٱلْحَمْدُ لِلَّهِ رَبِّ ٱلْعَٰلَمِينَ",
            exampleRef = "Al-Fatihah 1:7"
        ),
        WaqafRule(
            symbol = SAJDAH_SYM,
            arabicName = "سجدة",
            latinName = "Tanda Sajdah (Sujud Tilawah)",
            meaning = "Disunnahkan melakukan Sujud Tilawah ketika membaca atau mendengar ayat ini.",
            recommendation = "Lakukan sujud tilawah 1 kali (dalam atau luar shalat) saat membaca atau mendengar ayat ini.",
            actionCategory = ActionCategory.OPTIONAL,
            badgeColor = QuranColors.BadgeWaqafStop,
            detailedRule = "Ayat Sajdah adalah ayat Al-Qur'an yang memerintahkan atau mencontohkan sujud kepada Allah. Sunnah Muakkadah untuk bersujud tilawah.",
            exampleAyah = "إِنَّ ٱلَّذِينَ عِندَ رَبِّكَ لَا يَسْتَكْبِرُونَ عَنْ عِبَادَتِهِۦ وَيُسَبِّحُونَهُۥ وَلَهُۥ يَسْجُدُونَ ۩",
            exampleRef = "Al-A'raf 7:206"
        ),
        WaqafRule(
            symbol = AYAH_END_SYM,
            arabicName = "نهاية الآية",
            latinName = "Akhir Ayat (Waqaf Tam)",
            meaning = "Tanda akhir ayat. Disunnahkan berhenti.",
            recommendation = "Disunnahkan berhenti di setiap akhir ayat mengikuti sunnah bacaan Rasulullah SAW.",
            actionCategory = ActionCategory.PREFERRED_STOP,
            badgeColor = QuranColors.Secondary,
            detailedRule = "Berhenti pada akhir ayat merupakan wakaf tam (sempurna) yang dianjurkan dalam qiraat Al-Qur'an.",
            exampleAyah = "ٱلْحَمْدُ لِلَّهِ رَبِّ ٱلْعَٰلَمِينَ (٢)",
            exampleRef = "Al-Fatihah 1:2"
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
        return " (" + toArabicDigits(ayahNumber) + ") "
    }

    fun formatAyahTextWithEndMarker(ayahText: String, ayahNumber: Int): String {
        require(ayahNumber > 0) { "Nomor ayat harus positif" }
        val hasRuku = ayahText.contains(WAQAF_RUKU_SYM) || ayahText.contains("ࣖ") || ayahText.contains("\u08D6")
        val hasSajdah = ayahText.contains(SAJDAH_SYM) || ayahText.contains("\u06E9")

        val textWithoutTerminalMarker = ayahText
            .replace(Regex("\\s*([\\(﴿۝][٠-٩0-9]+[\\)﴾]?|۝[٠-٩0-9]*)\\s*$"), "")
            .replace("ࣖ", "")
            .replace("۩", "")
            .replace("\u08D6", "")
            .replace("\u06E9", "")
            .trimEnd()

        val ayahNumberStr = "(${toArabicDigits(ayahNumber)})"

        val endSuffix = when {
            hasRuku && hasSajdah -> " ۩ $ayahNumberStr ع "
            hasSajdah -> " ۩ $ayahNumberStr "
            hasRuku -> " $ayahNumberStr ع "
            else -> " $ayahNumberStr "
        }

        return "$textWithoutTerminalMarker$endSuffix"
    }

    /**
     * Removes the end-of-ayah sequence before rendering a separate
     * marker badge. This preserves all Tajwid/Waqaf annotations before it.
     */
    fun removeAyahEndMarker(text: AnnotatedString): AnnotatedString {
        val markerStart = text
            .getStringAnnotations(AYAH_END_ANNOTATION, 0, text.length)
            .firstOrNull()
            ?.start
            ?: text.text.indexOfFirst { it == '(' || it == '﴿' || it == AYAH_END_SYM.single() }

        if (markerStart < 0) return text
        val visibleEnd = text.text.substring(0, markerStart).trimEnd().length
        return text.subSequence(0, visibleEnd)
    }

    /**
     * Adds annotations only when a reviewed catalog can resolve each marker.
     * Glyph presence by itself is insufficient evidence for a semantic rule.
     */
    fun annotateWaqafMarkers(text: AnnotatedString): AnnotatedString {
        if (!SOURCE_CATALOG_VERIFIED) return text
        val builder = AnnotatedString.Builder(text)
        var i = 0
        while (i < text.length) {
            val char = text.text[i]
            if (char == '(' || char == '﴿' || char == AYAH_END_SYM.single()) {
                val start = i
                while (i < text.length) {
                    val nextChar = text.text[i]
                    i++
                    if (nextChar == ')' || nextChar == '﴾') break
                }
                builder.addStyle(
                    androidx.compose.ui.text.SpanStyle(color = QuranColors.Secondary),
                    start,
                    i
                )
                builder.addStringAnnotation(
                    AYAH_END_ANNOTATION,
                    AYAH_END_SYM,
                    start,
                    i
                )
                continue
            } else {
                findRuleBySymbol(char)?.let { rule ->
                    builder.addStyle(
                        androidx.compose.ui.text.SpanStyle(color = rule.badgeColor),
                        i,
                        i + 1
                    )
                    builder.addStringAnnotation(WAQAF_ANNOTATION, char.toString(), i, i + 1)
                    rule.pairId?.let { pairId ->
                        builder.addStringAnnotation(
                            WAQAF_PAIR_ANNOTATION,
                            pairId,
                            i,
                            i + 1
                        )
                    }
                }
                i++
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
            ?: if (char == 'ع' || char == 'ࣖ') ALL_WAQAF_RULES.firstOrNull { it.symbol == WAQAF_RUKU_SYM || it.symbol == "ࣖ" || it.symbol == "ع" } else null
    }
}
