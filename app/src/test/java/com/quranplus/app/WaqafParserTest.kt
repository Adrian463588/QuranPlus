package com.quranplus.app

import androidx.compose.ui.text.AnnotatedString
import com.quranplus.app.core.utils.WaqafParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WaqafParserTest {

    @Test
    fun GIVEN_unverifiedCatalog_WHEN_annotatingMarkers_THEN_textRemainsUnannotated() {
        val source = AnnotatedString("قۘل")

        val result = WaqafParser.annotateWaqafMarkers(source)

        assertEquals(source.text, result.text)
        assertEquals(0, result.getStringAnnotations(WaqafParser.WAQAF_ANNOTATION, 0, result.length).size)
        assertNull(WaqafParser.findRuleBySymbol('ۘ'))
    }

    @Test(expected = IllegalArgumentException::class)
    fun GIVEN_nonPositiveAyah_WHEN_formattingEndMarker_THEN_rejectsInvalidTarget() {
        WaqafParser.formatAyahEndMarker(0)
    }
}
