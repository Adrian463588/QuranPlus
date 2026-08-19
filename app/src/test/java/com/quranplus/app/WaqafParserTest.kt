package com.quranplus.app

import androidx.compose.ui.text.AnnotatedString
import com.quranplus.app.core.utils.WaqafParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
    }

    @Test(expected = IllegalArgumentException::class)
    fun GIVEN_nonPositiveAyah_WHEN_formattingEndMarker_THEN_rejectsInvalidTarget() {
        WaqafParser.formatAyahEndMarker(0)
    }
}
