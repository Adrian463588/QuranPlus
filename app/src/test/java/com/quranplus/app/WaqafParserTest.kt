package com.quranplus.app

import androidx.compose.ui.text.AnnotatedString
import com.quranplus.app.core.utils.WaqafParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WaqafParserTest {

    @Test
    fun GIVEN_verifiedCatalog_WHEN_annotatingMarkers_THEN_markerIsClickable() {
        val source = AnnotatedString("ق${WaqafParser.WAQAF_LAZIM_SYM}ل")

        val result = WaqafParser.annotateWaqafMarkers(source)

        assertEquals(source.text, result.text)
        assertEquals(1, result.getStringAnnotations(WaqafParser.WAQAF_ANNOTATION, 0, result.length).size)
        assertNotNull(WaqafParser.findRuleBySymbol(WaqafParser.WAQAF_LAZIM_SYM.first()))
    }

    @Test
    fun GIVEN_mushafGlyphs_WHEN_resolvingRules_THEN_mappingMatchesCatalog() {
        assertEquals("م", WaqafParser.findRuleBySymbol('ۘ')?.arabicName)
        assertEquals("لا", WaqafParser.findRuleBySymbol('ۙ')?.arabicName)
        assertEquals("ج", WaqafParser.findRuleBySymbol('ۚ')?.arabicName)
        assertEquals("صلى", WaqafParser.findRuleBySymbol('ۖ')?.arabicName)
        assertEquals("قلى", WaqafParser.findRuleBySymbol('ۗ')?.arabicName)
        assertEquals("ۛ ۛ", WaqafParser.findRuleBySymbol('ۛ')?.arabicName)
        assertEquals(
            WaqafParser.ActionCategory.MANDATORY,
            WaqafParser.findRuleBySymbol('ۘ')?.actionCategory
        )
        assertEquals(
            WaqafParser.ActionCategory.PREFERRED_STOP,
            WaqafParser.findRuleBySymbol('ۗ')?.actionCategory
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun GIVEN_nonPositiveAyah_WHEN_formattingEndMarker_THEN_rejectsInvalidTarget() {
        WaqafParser.formatAyahEndMarker(0)
    }

    @Test
    fun GIVEN_ayahEndMarker_WHEN_annotatingMarkers_THEN_isSeparatedFromWaqafAnnotations() {
        val source = AnnotatedString("الْحَمْدُ " + WaqafParser.formatAyahEndMarker(1))

        val result = WaqafParser.annotateWaqafMarkers(source)

        assertEquals(
            1,
            result.getStringAnnotations(
                WaqafParser.AYAH_END_ANNOTATION,
                0,
                result.length
            ).size
        )
        assertEquals(
            0,
            result.getStringAnnotations(
                WaqafParser.WAQAF_ANNOTATION,
                0,
                result.length
            ).size
        )
    }

    @Test
    fun GIVEN_sourceAlreadyHasEndMarker_WHEN_formattingAyahText_THEN_markerIsRenderedOnce() {
        val result = WaqafParser.formatAyahTextWithEndMarker("بِسْمِ ۝٧ ", 1)

        assertEquals(1, result.count { it == WaqafParser.AYAH_END_SYM.single() })
        assertEquals("بِسْمِ ۝١ ", result)
    }

    @Test
    fun GIVEN_annotatedAyah_WHEN_removingEndMarkerForBadge_THEN_keepsQuranTextOnly() {
        val source = WaqafParser.annotateWaqafMarkers(
            AnnotatedString("الْحَمْدُ " + WaqafParser.formatAyahEndMarker(2))
        )

        val result = WaqafParser.removeAyahEndMarker(source)

        assertEquals("الْحَمْدُ", result.text)
    }

    @Test
    fun GIVEN_ayahWithInternalWaqaf_WHEN_removingEndMarkerForBadge_THEN_preservesWaqaf() {
        val source = WaqafParser.annotateWaqafMarkers(
            AnnotatedString("لَا رَيْبَ ${WaqafParser.WAQAF_MUANAQAH_SYM} فِيهِ " +
                WaqafParser.formatAyahEndMarker(2))
        )

        val result = WaqafParser.removeAyahEndMarker(source)

        assertTrue(result.text.contains(WaqafParser.WAQAF_MUANAQAH_SYM))
        assertEquals(
            1,
            result.getStringAnnotations(WaqafParser.WAQAF_ANNOTATION, 0, result.length).size
        )
    }

    @Test
    fun GIVEN_muanaqahMarker_WHEN_annotatingMarkers_THEN_pairIdentityIsPreserved() {
        val source = AnnotatedString(
            "ق${WaqafParser.WAQAF_MUANAQAH_SYM}ل${WaqafParser.WAQAF_MUANAQAH_SYM}"
        )

        val result = WaqafParser.annotateWaqafMarkers(source)

        assertEquals(
            listOf("muanaqah", "muanaqah"),
            result.getStringAnnotations(
                WaqafParser.WAQAF_PAIR_ANNOTATION,
                0,
                result.length
            ).map { it.item }
        )
    }
}
