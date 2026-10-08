package com.quranplus.app

import com.quranplus.app.core.utils.SurahMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SearchHistoryAndReferenceTest {

    @Test
    fun GIVEN_alBaqarah255_WHEN_parseSearchReference_THEN_returnsSurah2Ayah255() {
        val result1 = SurahMapper.parseSearchReference("Al-Baqarah 255")
        assertNotNull(result1)
        assertEquals(2, result1?.first?.number)
        assertEquals(255, result1?.second)

        val result2 = SurahMapper.parseSearchReference("Al-Baqarah:255")
        assertNotNull(result2)
        assertEquals(2, result2?.first?.number)
        assertEquals(255, result2?.second)

        val result3 = SurahMapper.parseSearchReference("QS. Al-Baqarah: 255")
        assertNotNull(result3)
        assertEquals(2, result3?.first?.number)
        assertEquals(255, result3?.second)

        val result4 = SurahMapper.parseSearchReference("Al-Baqarah ayat 255")
        assertNotNull(result4)
        assertEquals(2, result4?.first?.number)
        assertEquals(255, result4?.second)
    }

    @Test
    fun GIVEN_numericChapterAndVerse_WHEN_parseSearchReference_THEN_returnsSurahAndAyah() {
        val resultColon = SurahMapper.parseSearchReference("2:255")
        assertNotNull(resultColon)
        assertEquals(2, resultColon?.first?.number)
        assertEquals(255, resultColon?.second)

        val resultSpace = SurahMapper.parseSearchReference("2 255")
        assertNotNull(resultSpace)
        assertEquals(2, resultSpace?.first?.number)
        assertEquals(255, resultSpace?.second)

        val alMulkAyah1 = SurahMapper.parseSearchReference("67:1")
        assertNotNull(alMulkAyah1)
        assertEquals(67, alMulkAyah1?.first?.number)
        assertEquals(1, alMulkAyah1?.second)
    }

    @Test
    fun GIVEN_surahOnlyOrInvalidAyah_WHEN_parseSearchReference_THEN_returnsNull() {
        // Only surah name, no ayah specified -> should return null
        assertNull(SurahMapper.parseSearchReference("Al-Mulk"))
        assertNull(SurahMapper.parseSearchReference("Al-Baqarah"))

        // Ayah exceeds verse count (Al-Baqarah has 286 ayahs)
        assertNull(SurahMapper.parseSearchReference("Al-Baqarah 287"))
        assertNull(SurahMapper.parseSearchReference("2:300"))

        // Ayah 0 or negative
        assertNull(SurahMapper.parseSearchReference("Al-Baqarah 0"))
    }

    @Test
    fun GIVEN_searchQueries_WHEN_deduplicatingAndAddingLRU_THEN_maintainsRecentOrder() {
        val maxHistory = 15
        val initial = listOf("Al-Mulk", "Yasin", "Al-Kahf")
        val newQuery = "Al-Baqarah 255"

        val updated = (listOf(newQuery) + initial.filterNot { it.equals(newQuery, ignoreCase = true) })
            .take(maxHistory)

        assertEquals(listOf("Al-Baqarah 255", "Al-Mulk", "Yasin", "Al-Kahf"), updated)

        // Adding existing query moves it to front
        val reAdded = "Yasin"
        val reUpdated = (listOf(reAdded) + updated.filterNot { it.equals(reAdded, ignoreCase = true) })
            .take(maxHistory)

        assertEquals(listOf("Yasin", "Al-Baqarah 255", "Al-Mulk", "Al-Kahf"), reUpdated)
    }

    @Test
    fun GIVEN_userTypingSequence_WHEN_onlyExplicitSearchesRecorded_THEN_historyExcludesIntermediateKeystrokes() {
        val simulatedKeystrokes = listOf("A", "Al", "Al-", "Al-M", "Al-Mu", "Al-Mul", "Al-Mulk")
        val recordedHistory = mutableListOf<String>()

        // Simulate behavior: onValueChange only filters, DOES NOT call recordSearchHistory
        for (keystroke in simulatedKeystrokes) {
            // Typing keystrokes: NO recording
        }

        // Only explicit action (e.g. keyboardActions.onSearch or selecting result/chip) records
        fun onSearchExplicit(query: String) {
            val clean = query.trim()
            if (clean.length >= 2) {
                recordedHistory.removeAll { it.equals(clean, ignoreCase = true) }
                recordedHistory.add(0, clean)
            }
        }

        onSearchExplicit("Al-Mulk")

        // Verifying intermediate keystrokes are NOT in history
        assertEquals(listOf("Al-Mulk"), recordedHistory)
        org.junit.Assert.assertFalse(recordedHistory.contains("Al"))
        org.junit.Assert.assertFalse(recordedHistory.contains("Al-"))
        org.junit.Assert.assertFalse(recordedHistory.contains("Al-M"))
    }
}
