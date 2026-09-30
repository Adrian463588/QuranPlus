package com.quranplus.app

import com.quranplus.app.core.ui.theme.QuranColors
import com.quranplus.app.features.dzikir.data.DzikirDataCatalog
import com.quranplus.app.features.dzikir.data.DzikirTajwidFormatter
import org.junit.Assert.assertTrue
import org.junit.Test

class DzikirTajwidColorAuditTest {

    @Test
    fun GIVEN_catalog_WHEN_formattingEveryEntry_THEN_rendersColoredTajwidSpans() {
        DzikirDataCatalog.ALL_ITEMS.forEach { item ->
            val formatted = DzikirTajwidFormatter.formatArabic(
                arabicText = item.arabicText,
                tajwidTags = item.tajwidTags,
                enableTajwid = true
            )
            val nonBaseSpans = formatted.spanStyles.filter { it.item.color != QuranColors.TextArabicDefault }
            assertTrue("${item.id} should render colored Tajwid spans", nonBaseSpans.isNotEmpty())
        }
    }

    @Test
    fun GIVEN_partialIqlabSourceTag_WHEN_formattingPair_THEN_colorsUncoveredFollowingLetter() {
        val text = "مِنْ بَعْدِ"
        val formatted = DzikirTajwidFormatter.formatArabic(
            arabicText = text,
            tajwidTags = "[i[مِنْ]] بَعْدِ"
        )
        val baStart = text.indexOf('ب')

        assertTrue(
            "The Iqlab color should cover the following Ba even when the source tag covers Nun only",
            formatted.spanStyles.any {
                it.item.color == QuranColors.TajwidIqlab && it.start <= baStart && it.end >= baStart + 2
            }
        )
    }

    @Test
    fun GIVEN_equalLengthMismatchedSource_WHEN_formattingArabic_THEN_doesNotApplyColorAtWrongOffsets() {
        val formatted = DzikirTajwidFormatter.formatArabic(
            arabicText = "كَتَبَ",
            tajwidTags = "[n[قَالَ]]"
        )

        assertTrue(
            "Unaligned source marks must not color display characters by coincidental string length",
            formatted.spanStyles.none { it.item.color != QuranColors.TextArabicDefault }
        )
    }
}
