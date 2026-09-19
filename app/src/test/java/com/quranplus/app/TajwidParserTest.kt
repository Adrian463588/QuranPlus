package com.quranplus.app

import com.quranplus.app.core.utils.TajwidParser
import com.quranplus.app.core.utils.TajwidTagCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TajwidParserTest {

    @Test
    fun GIVEN_plainArabic_WHEN_buildColoredAyahText_THEN_returnsAnnotatedString() {
        val plainText = "إِنَّ اللَّهَ مَعَ الصَّابِرِينَ وَقُلْ رَبِّ أَعُوذُ بِكَ مِنْ هَمَزَاتِ الشَّيَاطِينِ"
        val annotated = TajwidParser.buildColoredAyahText(
            arabicText = plainText,
            tajwidTags = null,
            enableTajwid = true
        )

        assertNotNull(annotated)
        assertTrue(annotated.text.isNotEmpty())
        assertTrue(
            annotated.getStringAnnotations(TajwidParser.TAJWID_ANNOTATION, 0, annotated.length).isEmpty()
        )
    }

    @Test
    fun GIVEN_plainArabicWithoutSourceTags_WHEN_extractOccurrences_THEN_returnsEmpty() {
        val occurrences = TajwidParser.extractTajwidOccurrences("إِنَّ اللَّهَ مَعَ الصَّابِرِينَ")

        assertTrue(occurrences.isEmpty())
    }

    @Test
    fun GIVEN_disabledTajwid_WHEN_buildColoredAyahText_THEN_returnsPlainText() {
        val plainText = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"
        val annotated = TajwidParser.buildColoredAyahText(
            arabicText = plainText,
            tajwidTags = null,
            enableTajwid = false
        )

        assertNotNull(annotated)
        assertTrue(annotated.text == plainText)
    }

    @Test
    fun GIVEN_databaseBracketTags_WHEN_parseBracketTags_THEN_preservesTextAndSourceSpans() {
        val taggedText = "[h:1[ٱ] [l[ل] [n[ـٰ]"

        val parsed = TajwidParser.parseBracketTags(taggedText)

        assertEquals("ٱ ل ٰ", parsed.text)
        assertEquals(3, parsed.spans.size)
        assertEquals(TajwidParser.TajwidType.HAMZAT_WASL, parsed.spans[0].type)
        assertEquals("h:1", parsed.spans[0].sourceTag)
        assertEquals(TajwidParser.TajwidType.LAM_SHAMSIYYAH, parsed.spans[1].type)
        assertEquals(TajwidParser.TajwidType.MAD_TABII, parsed.spans[2].type)
        assertTrue(parsed.unknownTags.isEmpty())
        assertFalse(parsed.malformed)
    }

    @Test
    fun GIVEN_bundledBismillahMarkup_WHEN_parseBracketTags_THEN_matchesStoredArabicCodepoints() {
        val taggedText =
            "بِسْمِ [h:1[ٱ]للَّهِ [h:2[ٱ][l[ل]رَّحْمَ[n[ـٰ]نِ " +
                "[h:3[ٱ][l[ل]رَّح[p[ِي]مِ"

        val parsed = TajwidParser.parseBracketTags(taggedText)

        assertEquals("بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ", parsed.text)
        assertEquals(7, parsed.spans.size)
        assertTrue(parsed.unknownTags.isEmpty())
    }

    @Test
    fun GIVEN_databaseBracketTagsWithVerseSuffix_WHEN_buildColoredAyahText_THEN_annotationsRemainClickable() {
        val taggedText = "[h:1[ٱ] [q[قْ]"
        val parsed = TajwidParser.parseBracketTags(taggedText)
        val annotated = TajwidParser.buildColoredAyahText(
            arabicText = "${parsed.text} (١) ",
            tajwidTags = taggedText,
            enableTajwid = true
        )

        assertEquals("ٱ قْ (١) ", annotated.text)
        assertEquals(
            "HAMZAT_WASL",
            annotated.getStringAnnotations(
                TajwidParser.TAJWID_ANNOTATION,
                0,
                1
            ).single().item
        )
        assertEquals(
            "q",
            annotated.getStringAnnotations(
                TajwidParser.TAJWID_SOURCE_ANNOTATION,
                2,
                4
            ).single().item
        )
    }

    @Test
    fun GIVEN_unknownBracketTag_WHEN_parseBracketTags_THEN_failsClosedWithoutGuessing() {
        val parsed = TajwidParser.parseBracketTags("[z[ب]")

        assertEquals("ب", parsed.text)
        assertTrue(parsed.spans.isEmpty())
        assertEquals(setOf("z"), parsed.unknownTags)
        assertFalse(parsed.malformed)
    }

    @Test
    fun GIVEN_unknownBracketTag_WHEN_buildColoredAyahText_THEN_noKnownSpanIsRendered() {
        val annotated = TajwidParser.buildColoredAyahText(
            arabicText = "بِسْمِ",
            tajwidTags = "[z[بِسْمِ]"
        )

        assertTrue(
            annotated.getStringAnnotations(TajwidParser.TAJWID_ANNOTATION, 0, annotated.length)
                .isEmpty()
        )
    }

    @Test
    fun GIVEN_sourceAndDisplayCharactersDoNotMatch_WHEN_buildColoredAyahText_THEN_colorIsNotFabricated() {
        val annotated = TajwidParser.buildColoredAyahText(
            arabicText = "ت",
            tajwidTags = "[h[ب]",
            enableTajwid = true
        )

        assertEquals("ت", annotated.text)
        assertTrue(annotated.getStringAnnotations(TajwidParser.TAJWID_ANNOTATION, 0, 1).isEmpty())
    }

    @Test
    fun GIVEN_nestedSourceTags_WHEN_parseBracketTags_THEN_preservesInnerAndOuterSpans() {
        val parsed = TajwidParser.parseBracketTags("[o[ُوٓ[s[اْ]ۚ]")

        assertEquals("ُوٓاْۚ", parsed.text)
        assertFalse(parsed.malformed)
        assertEquals(2, parsed.spans.size)
        assertTrue(parsed.spans.any { it.type == TajwidParser.TajwidType.MAD_WAJIB_JAIZ })
        assertTrue(parsed.spans.any { it.type == TajwidParser.TajwidType.SILENT })
    }

    @Test
    fun GIVEN_reviewedSourceTagCatalog_WHEN_resolvingTags_THEN_everyTagHasTypedRule() {
        TajwidTagCatalog.mappings.forEach { mapping ->
            assertNotNull(TajwidParser.TajwidType.fromSourceTag(mapping.sourceTag))
        }
    }

    @Test
    fun GIVEN_surah3Ayah4WithTatweelAndTanwin_WHEN_buildColoredAyahText_THEN_appliesTajwidColorsAccurately() {
        val displayArabic = "مِن قَبْلُ هُدًۭى لِّلنَّاسِ وَأَنزَلَ ٱلْفُرْقَانَ ۗ إِنَّ ٱلَّذِينَ كَفَرُوا۟ بِـَٔايَٰتِ ٱللَّهِ لَهُمْ عَذَابٌۭ شَدِيدٌۭ ۗ وَٱللَّهُ عَزِيزٌۭ ذُو ٱنتِقَامٍۗ (٤) "
        val tajwidTags = "مِ[f:128[ن ق]َ[q:129[بْ]لُ هُ[u:968[دًى ل]ِّل[g[نّ]َاسِ وَأَ[f:91[نز]َلَ [h:1726[ٱ]لْفُرْقَانَ\u200cۗ إِ[g[نّ]َ [h:24[ٱ]لَّذِينَ كَفَرُو[s[اْ] بِـَٔـايَ[n[ـٰ]تِ [h:322[ٱ]للَّهِ لَهُمْ عَذَا[f:1727[بٌ ش]َدِي[a:1728[دٌ\u200cۗ و]َ[h:72[ٱ]للَّهُ عَزِي[f:1729[زٌ ذ]ُو [h:1730[ٱ][f:1731[نت]ِق[p[َا]مٍ"

        val annotated = TajwidParser.buildColoredAyahText(
            arabicText = displayArabic,
            tajwidTags = tajwidTags,
            enableTajwid = true
        )

        assertNotNull(annotated)
        val tajwidAnnotations = annotated.getStringAnnotations(TajwidParser.TAJWID_ANNOTATION, 0, annotated.length)
        assertTrue("Tajweed annotations should not be empty for Surah 3:4", tajwidAnnotations.isNotEmpty())
        assertTrue(tajwidAnnotations.any { it.item == "IKHFA_HAQIQI" })
        assertTrue(tajwidAnnotations.any { it.item == "QALQALAH" })
        assertTrue(tajwidAnnotations.any { it.item == "IDGHAM_BILAGHUNNAH" })
        assertTrue(tajwidAnnotations.any { it.item == "GHUNNAH" })
    }

    @Test
    fun GIVEN_surah2Ayah4WithAlifHamzaVariants_WHEN_buildColoredAyahText_THEN_alignsAndColors() {
        val displayArabic = "وَٱلَّذِينَ يُؤْمِنُونَ بِمَآ أُنزِلَ إِلَيْكَ وَمَآ أُنزِلَ مِن قَبْلِكَ وَبِٱلْءَاخِرَةِ هُمْ يُوقِنُونَ (٤) "
        val tajwidTags = "وَ[h:9999[ٱ]لَّذِينَ يُؤْمِنُونَ بِم[o[َآ] أُ[f:17[نز]ِلَ إِلَيْكَ وَم[o[َآ] أُ[f:17[نز]ِلَ مِ[f:18[ن ق]َ[q:19[بْ]لِكَ وَبِ[h:20[ٱ]لْأَخِرَةِ هُمْ يُوقِن[p[ُو]نَ"

        val annotated = TajwidParser.buildColoredAyahText(
            arabicText = displayArabic,
            tajwidTags = tajwidTags,
            enableTajwid = true
        )

        assertNotNull(annotated)
        val tajwidAnnotations = annotated.getStringAnnotations(TajwidParser.TAJWID_ANNOTATION, 0, annotated.length)
        assertTrue("Tajweed annotations should not be empty for Surah 2:4", tajwidAnnotations.isNotEmpty())
    }
}
