package com.quranplus.app

import com.quranplus.app.core.ui.theme.QuranColors
import com.quranplus.app.features.dzikir.data.DzikirDataCatalog
import com.quranplus.app.features.dzikir.data.DzikirTajwidFormatter
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class DzikirTajwidColorAuditTest {

    @Test
    fun GIVEN_catalog_WHEN_togglingTajwid_THEN_preservesEveryArabicLetterHarakatAndStop() {
        DzikirDataCatalog.ALL_ITEMS.forEach { item ->
            listOf(true, false).forEach { enabled ->
                val formatted = DzikirTajwidFormatter.formatArabic(item.arabicText, item.tajwidTags, enabled)
                assertEquals(item.id, item.arabicText, formatted.text)
            }
        }
    }

    @Test
    fun GIVEN_vowelBeforeShaddah_WHEN_formatting_THEN_colorsTheWholeGhunnahCluster() {
        listOf("إِنَّ", "إِنَّ").forEach { text ->
            val formatted = DzikirTajwidFormatter.formatArabic(text)
            val start = text.indexOf('ن')
            assertTrue(formatted.spanStyles.any {
                it.item.color == QuranColors.TajwidGhunnah && it.start == start && it.end == text.length
            })
            assertEquals(text, formatted.text)
        }
    }

    @Test
    fun GIVEN_stopBoundary_WHEN_detectingConnectedRules_THEN_doesNotColorFollowingBa() {
        listOf("۝", "ۘ", "ۜ", "\n", ".", "،").forEach { stop ->
            listOf("مِنْ", "هُمْ").forEach { word ->
                val text = "$word $stop بَعْدِ"
                val formatted = DzikirTajwidFormatter.formatArabic(text)
                val ba = text.indexOf('ب')
                assertTrue("$text: stop must separate connected tajwid", formatted.spanStyles.none {
                    it.item.color != QuranColors.TextArabicDefault && it.start <= ba && it.end > ba
                })
                assertEquals(text, formatted.text)
            }
        }
    }

    @Test
    fun GIVEN_optionalWaqaf_WHEN_readingContinuously_THEN_keepsConnectedIqlab() {
        val text = "مِنْ ۚ بَعْدِ"
        val formatted = DzikirTajwidFormatter.formatArabic(text)
        val ba = text.indexOf('ب')
        assertTrue(formatted.spanStyles.any {
            it.item.color == QuranColors.TajwidIqlab && it.start <= ba && it.end > ba
        })
    }

    @Test
    fun GIVEN_reviewedHizib_WHEN_readingLetterNames_THEN_keepsLongVowelMarks() {
        val items = DzikirDataCatalog.ALL_ITEMS.associateBy { it.id }
        assertEquals(3, Regex("كٓهٰيٰعٓصٓ").findAll(items.getValue("bahr_03").arabicText).count())
        assertTrue(items.getValue("bahr_06").arabicText.contains("طٰسٓ"))
        assertTrue(items.getValue("bahr_06").arabicText.contains("حٰمٓ"))
        assertTrue(items.getValue("bahr_07").arabicText.contains("يٰسٓ سَقْفُنَا"))
        assertTrue(items.getValue("bahr_07").arabicText.contains("عٓسٓقٓ حِمَايَتُنَا"))
    }

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
