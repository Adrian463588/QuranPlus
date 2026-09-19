package com.quranplus.app

import com.quranplus.app.core.utils.SurahMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SurahAndAyahSearchTest {

    @Test
    fun GIVEN_alMulkAndVariants_WHEN_matchingSurah_THEN_surah67Matches() {
        assertTrue(SurahMapper.matchesSurah("Al-Mulk", 67, "Al-Mulk"))
        assertTrue(SurahMapper.matchesSurah("mulk", 67, "Al-Mulk"))
        assertTrue(SurahMapper.matchesSurah("al mulk", 67, "Al-Mulk"))
        assertTrue(SurahMapper.matchesSurah("67", 67, "Al-Mulk"))

        val found = SurahMapper.findSurah("Al-Mulk")
        assertNotNull(found)
        assertEquals(67, found?.number)
        assertEquals(30, found?.ayahCount)
    }

    @Test
    fun GIVEN_adDuhaAndVariants_WHEN_matchingSurah_THEN_surah93Matches() {
        assertTrue(SurahMapper.matchesSurah("Ad-Duha", 93, "Ad-Duha"))
        assertTrue(SurahMapper.matchesSurah("duha", 93, "Ad-Duha"))
        assertTrue(SurahMapper.matchesSurah("ad duha", 93, "Ad-Duha"))
        assertTrue(SurahMapper.matchesSurah("93", 93, "Ad-Duha"))

        // Database has "Ad-Dhuhaa", user queries "Ad-Duha" or "duha"
        assertTrue(SurahMapper.matchesSurah("Ad-Duha", 93, "Ad-Dhuhaa"))
        assertTrue(SurahMapper.matchesSurah("duha", 93, "Ad-Dhuhaa"))
        assertTrue(SurahMapper.matchesSurah("ad duha", 93, "Ad-Dhuhaa"))

        val found = SurahMapper.findSurah("duha")
        assertNotNull(found)
        assertEquals(93, found?.number)
        assertEquals(11, found?.ayahCount)
    }

    @Test
    fun GIVEN_alBaqarahAndMaidah_WHEN_matchingSurah_THEN_matchesAccurately() {
        assertTrue(SurahMapper.matchesSurah("Al-Baqarah", 2, "Al-Baqarah"))
        assertTrue(SurahMapper.matchesSurah("baqarah", 2, "Al-Baqarah"))
        assertTrue(SurahMapper.matchesSurah("2", 2, "Al-Baqarah"))

        assertTrue(SurahMapper.matchesSurah("Al-Ma'idah", 5, "Al-Ma'idah"))
        assertTrue(SurahMapper.matchesSurah("maidah", 5, "Al-Ma'idah"))
        assertTrue(SurahMapper.matchesSurah("5", 5, "Al-Ma'idah"))
    }

    @Test
    fun GIVEN_ayahNumbers_WHEN_validatingAgainstSurahAyahCount_THEN_validatesBounds() {
        val baqarah = SurahMapper.getSurah(2)
        assertNotNull(baqarah)
        assertEquals(286, baqarah?.ayahCount)
        assertTrue(255 in 1..(baqarah?.ayahCount ?: 0))
        assertFalse(287 in 1..(baqarah?.ayahCount ?: 0))

        val maidah = SurahMapper.getSurah(5)
        assertNotNull(maidah)
        assertEquals(120, maidah?.ayahCount)
        assertTrue(60 in 1..(maidah?.ayahCount ?: 0))
        assertFalse(121 in 1..(maidah?.ayahCount ?: 0))

        val duha = SurahMapper.getSurah(93)
        assertNotNull(duha)
        assertEquals(11, duha?.ayahCount)
        assertTrue(11 in 1..(duha?.ayahCount ?: 0))
        assertFalse(12 in 1..(duha?.ayahCount ?: 0))
    }
}
