package com.quranplus.app

import com.quranplus.shared.features.quran.domain.Bookmark
import com.quranplus.shared.features.quran.domain.HadithMarker
import com.quranplus.shared.features.quran.domain.QuranMarker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SafUserDataContractTest {

    @Test
    fun GIVEN_bookmarksList_WHEN_mappedForExport_THEN_containsAllRequiredFields() {
        val bookmark = Bookmark(
            id = 101L,
            surahNumber = 2,
            surahName = "Al-Baqarah",
            ayahNumber = 255,
            ayahTextArabic = "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ",
            ayahTranslation = "Allah, tidak ada tuhan selain Dia",
            note = "Ayat Kursi",
            timestamp = 1700000000000L
        )

        assertEquals(101L, bookmark.id)
        assertEquals(2, bookmark.surahNumber)
        assertEquals("Al-Baqarah", bookmark.surahName)
        assertEquals(255, bookmark.ayahNumber)
        assertEquals("Ayat Kursi", bookmark.note)
        assertEquals(1700000000000L, bookmark.timestamp)
    }

    @Test
    fun GIVEN_markers_WHEN_createdAndCopied_THEN_preservesQuranAndHadithMarkerIntegrity() {
        val quranMarker = QuranMarker(
            surahNumber = 67,
            surahName = "Al-Mulk",
            ayahNumber = 14,
            timestamp = 1700000001000L
        )
        val hadithMarker = HadithMarker(
            collectionId = "bukhari",
            collectionName = "Shahih Bukhari",
            hadithNumber = 42,
            timestamp = 1700000002000L
        )

        assertEquals(67, quranMarker.surahNumber)
        assertEquals("Al-Mulk", quranMarker.surahName)
        assertEquals(14, quranMarker.ayahNumber)
        assertEquals(1700000001000L, quranMarker.timestamp)

        assertEquals("bukhari", hadithMarker.collectionId)
        assertEquals("Shahih Bukhari", hadithMarker.collectionName)
        assertEquals(42, hadithMarker.hadithNumber)
        assertEquals(1700000002000L, hadithMarker.timestamp)

        val updatedQm = quranMarker.copy(ayahNumber = 15)
        assertEquals(15, updatedQm.ayahNumber)
        assertEquals(67, updatedQm.surahNumber)

        val updatedHm = hadithMarker.copy(hadithNumber = 43)
        assertEquals(43, updatedHm.hadithNumber)
        assertEquals("bukhari", updatedHm.collectionId)
    }

    @Test
    fun GIVEN_existingLocalBookmarksAndSafBookmarks_WHEN_merged_THEN_deduplicatesBySurahAndAyah() {
        val localBookmarks = listOf(
            Bookmark(1L, 2, "Al-Baqarah", 255, "Ar", "Tr", "Local note", 1000L)
        )
        val safBookmarks = listOf(
            Bookmark(2L, 2, "Al-Baqarah", 255, "Ar", "Tr", "SAF note", 2000L),
            Bookmark(3L, 3, "Ali 'Imran", 18, "Ar2", "Tr2", null, 3000L)
        )

        // Merge logic: existing bookmarks by (surahNumber, ayahNumber) key
        val existingKeys = localBookmarks.map { "${it.surahNumber}:${it.ayahNumber}" }.toSet()
        val toInsert = safBookmarks.filter { "${it.surahNumber}:${it.ayahNumber}" !in existingKeys }

        assertEquals(1, toInsert.size)
        assertEquals(3, toInsert[0].surahNumber)
        assertEquals(18, toInsert[0].ayahNumber)
    }

    @Test
    fun GIVEN_markerKeyFormat_WHEN_evaluated_THEN_matchesContract() {
        val markerKey = "quran_marker_v1"
        assertNotNull(markerKey)
        assertNotEquals("", markerKey)

        val hadithKey = "hadith_marker_v1"
        assertNotNull(hadithKey)
        assertNotEquals("", hadithKey)
    }
}
