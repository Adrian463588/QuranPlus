package com.quranplus.app

import com.quranplus.app.core.utils.TajwidParser
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
}
