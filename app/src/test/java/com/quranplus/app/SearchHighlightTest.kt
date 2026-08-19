package com.quranplus.app

import androidx.compose.ui.graphics.Color
import com.quranplus.app.features.quran.presentation.highlightSearchMatches
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchHighlightTest {

    @Test
    fun GIVEN_queryTerm_WHEN_highlightingSearchText_THEN_matchingRangeIsAnnotated() {
        val result = highlightSearchMatches(
            text = "Allah Maha Pengasih",
            query = "allah",
            highlightColor = Color.Yellow,
            highlightTextColor = Color.Black
        )

        assertEquals("Allah Maha Pengasih", result.text)
        assertEquals(1, result.spanStyles.size)
        assertEquals(0, result.spanStyles.single().start)
        assertEquals(5, result.spanStyles.single().end)
    }

    @Test
    fun GIVEN_blankQuery_WHEN_highlightingSearchText_THEN_textIsUnchangedWithoutAnnotation() {
        val result = highlightSearchMatches(
            text = "Allah Maha Pengasih",
            query = "",
            highlightColor = Color.Yellow,
            highlightTextColor = Color.Black
        )

        assertEquals("Allah Maha Pengasih", result.text)
        assertTrue(result.spanStyles.isEmpty())
    }
}
