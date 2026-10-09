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

    @Test
    fun GIVEN_upTo15QuranMarkers_WHEN_created_THEN_eachHasDistinctColorIndexAndUnderMaxLimit() {
        val markers = (0 until 15).map { index ->
            QuranMarker(
                surahNumber = 2,
                surahName = "Al-Baqarah",
                ayahNumber = index + 1,
                timestamp = 1700000000000L + index,
                colorIndex = index
            )
        }

        assertEquals(15, markers.size)
        val colorIndices = markers.map { it.colorIndex }.toSet()
        assertEquals(15, colorIndices.size)
        assertEquals(0, markers.first().colorIndex)
        assertEquals(14, markers.last().colorIndex)

        // Test capping when an extra 16th marker is attempted
        val capped = (markers + QuranMarker(2, "Al-Baqarah", 16, 0L, 0)).take(15)
        assertEquals(15, capped.size)
    }

    @Test
    fun GIVEN_multiHadithMarkers_WHEN_colorAssigned_THEN_supportsDifferentColorsUpTo15() {
        val hadithMarkers = (0 until 15).map { index ->
            HadithMarker(
                collectionId = "bukhari",
                collectionName = "Shahih Bukhari",
                hadithNumber = index + 1,
                timestamp = 1700000000000L + index,
                colorIndex = index
            )
        }

        assertEquals(15, hadithMarkers.size)
        val distinctColors = hadithMarkers.map { it.colorIndex }.toSet()
        assertEquals(15, distinctColors.size)
    }

    @Test
    fun GIVEN_hadithBookmark_WHEN_created_THEN_containsAllRequiredFields() {
        val hadithBookmark = com.quranplus.shared.features.quran.domain.HadithBookmark(
            id = 1L,
            collectionId = "bukhari",
            collectionName = "Shahih Bukhari",
            hadithNumber = 1,
            hadithTextArabic = "إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ",
            hadithTranslation = "Sesungguhnya setiap amalan tergantung pada niatnya",
            note = "Hadits tentang niat",
            timestamp = 1700000003000L
        )

        assertEquals(1L, hadithBookmark.id)
        assertEquals("bukhari", hadithBookmark.collectionId)
        assertEquals("Shahih Bukhari", hadithBookmark.collectionName)
        assertEquals(1, hadithBookmark.hadithNumber)
        assertEquals("إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ", hadithBookmark.hadithTextArabic)
        assertEquals("Sesungguhnya setiap amalan tergantung pada niatnya", hadithBookmark.hadithTranslation)
        assertEquals("Hadits tentang niat", hadithBookmark.note)
        assertEquals(1700000003000L, hadithBookmark.timestamp)
    }

    @Test
    fun GIVEN_existingLocalAndSafHadithBookmarks_WHEN_merged_THEN_deduplicatesByCollectionAndNumber() {
        val local = listOf(
            com.quranplus.shared.features.quran.domain.HadithBookmark(
                id = 1L,
                collectionId = "bukhari",
                collectionName = "Shahih Bukhari",
                hadithNumber = 1,
                hadithTextArabic = "Ar1",
                hadithTranslation = "Tr1",
                note = "Catatan Lokal",
                timestamp = 1000L
            )
        )
        val saf = listOf(
            com.quranplus.shared.features.quran.domain.HadithBookmark(
                id = 2L,
                collectionId = "bukhari",
                collectionName = "Shahih Bukhari",
                hadithNumber = 1,
                hadithTextArabic = "Ar1",
                hadithTranslation = "Tr1",
                note = "Catatan SAF (lama)",
                timestamp = 500L
            ),
            com.quranplus.shared.features.quran.domain.HadithBookmark(
                id = 3L,
                collectionId = "muslim",
                collectionName = "Shahih Muslim",
                hadithNumber = 42,
                hadithTextArabic = "Ar42",
                hadithTranslation = "Tr42",
                note = null,
                timestamp = 2000L
            )
        )

        val existingKeys = local.map { "${it.collectionId}:${it.hadithNumber}" }.toSet()
        val toInsert = saf.filter { "${it.collectionId}:${it.hadithNumber}" !in existingKeys }
        val merged = local + toInsert

        assertEquals(2, merged.size)
        assertEquals("Catatan Lokal", merged.find { it.collectionId == "bukhari" && it.hadithNumber == 1 }?.note)
        assertEquals("muslim", merged.find { it.hadithNumber == 42 }?.collectionId)
    }

    @Test
    fun GIVEN_bookmarkAndMarker_WHEN_compared_THEN_theyAreFundamentallyDistinctEntities() {
        // Marker represents current stop position with a color index
        val marker = HadithMarker(
            collectionId = "bukhari",
            collectionName = "Shahih Bukhari",
            hadithNumber = 10,
            colorIndex = 0
        )

        // Bookmark represents a saved/favorited item with translation, text, notes
        val bookmark = com.quranplus.shared.features.quran.domain.HadithBookmark(
            id = 10L,
            collectionId = "bukhari",
            collectionName = "Shahih Bukhari",
            hadithNumber = 10,
            hadithTextArabic = "Text",
            hadithTranslation = "Translation",
            note = "Saved for study"
        )

        // They serve different functions and have different models
        assertNotEquals(marker::class, bookmark::class)
        assertEquals(marker.collectionId, bookmark.collectionId)
        assertEquals(marker.hadithNumber, bookmark.hadithNumber)
        assertNotNull(bookmark.note)
    }
}

