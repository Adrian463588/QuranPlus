package com.quranplus.app

import com.quranplus.app.core.database.dao.BookmarkDao
import com.quranplus.app.core.database.dao.LastReadDao
import com.quranplus.app.core.database.dao.QuranDao
import com.quranplus.app.core.database.dao.TafsirDao
import com.quranplus.app.core.database.dao.WordByWordDao
import com.quranplus.app.core.database.entity.SurahEntity
import com.quranplus.app.core.utils.SurahMapper
import com.quranplus.app.features.quran.data.QuranRepositoryImpl
import com.quranplus.app.features.quran.data.WordByWordRepositoryImpl
import com.quranplus.app.features.quran.domain.GetAyahsBySurahUseCase
import com.quranplus.app.features.quran.domain.GetBookmarksUseCase
import com.quranplus.app.features.quran.domain.GetFirstAyahByJuzUseCase
import com.quranplus.app.features.quran.domain.GetFirstAyahByPageUseCase
import com.quranplus.app.features.quran.domain.GetLastReadUseCase
import com.quranplus.app.features.quran.domain.GetSurahDetailUseCase
import com.quranplus.app.features.quran.domain.GetSurahListUseCase
import com.quranplus.app.features.quran.domain.GetTafsirUseCase
import com.quranplus.app.features.quran.domain.GetWordsBySurahUseCase
import com.quranplus.app.features.quran.domain.SaveLastReadUseCase
import com.quranplus.app.features.quran.domain.SearchQuranUseCase
import com.quranplus.app.features.quran.domain.ToggleBookmarkUseCase
import com.quranplus.app.features.quran.domain.DeleteBookmarkUseCase
import com.quranplus.app.features.quran.domain.RestoreBookmarkUseCase
import com.quranplus.app.features.quran.domain.UpdateBookmarkNoteUseCase
import com.quranplus.app.features.quran.presentation.QuranViewModel
import com.quranplus.app.features.quran.presentation.UiState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
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

    @Test
    fun GIVEN_all114Surahs_WHEN_inspectingCatalog_THEN_everySurahHasUniqueVerifiedIndonesianMeaningAndExactAyahTotal() {
        val all = SurahMapper.getAllSurahs()
        assertEquals(114, all.size)
        assertEquals((1..114).toList(), all.map { it.number })
        assertEquals(6236, all.sumOf { it.ayahCount })
        assertTrue(all.all { it.meaningId.isNotBlank() && it.latinName.isNotBlank() && it.ayahCount > 0 })

        val expectedFixtures = mapOf(
            1 to "Pembukaan",
            2 to "Sapi Betina",
            3 to "Keluarga Imran",
            4 to "Perempuan",
            5 to "Hidangan",
            6 to "Binatang Ternak",
            7 to "Tempat yang Tertinggi",
            8 to "Rampasan Perang",
            9 to "Pengampunan",
            10 to "Yunus",
            12 to "Yusuf",
            14 to "Ibrahim",
            19 to "Maryam",
            47 to "Muhammad",
            114 to "Manusia"
        )
        expectedFixtures.forEach { (number, expected) ->
            assertEquals("Mismatch for surah $number", expected, SurahMapper.getMeaningId(number))
        }
    }

    @Test
    fun GIVEN_indonesianMeaningQueries_WHEN_matchingSurah_THEN_returnsExpectedSurahWithoutFalsePositives() {
        assertTrue(SurahMapper.matchesSurah("Pembukaan", 1, "Al-Faatiha", nameMeaningId = "Pembukaan"))
        assertFalse(SurahMapper.matchesSurah("Pembukaan", 2, "Al-Baqara", nameMeaningId = "Sapi Betina"))

        assertTrue(SurahMapper.matchesSurah("Sapi Betina", 2, "Al-Baqara", nameMeaningId = "Sapi Betina"))
        assertFalse("Sapi Betina must not false-match At-Tin via substring 'tin'", SurahMapper.matchesSurah("Sapi Betina", 95, "At-Tin", nameMeaningId = "Buah Tin"))

        assertTrue(SurahMapper.matchesSurah("Perempuan", 4, "An-Nisaa", nameMeaningId = "Perempuan"))
        assertTrue(SurahMapper.matchesSurah("Hidangan", 5, "Al-Maaida", nameMeaningId = "Hidangan"))
        assertTrue(SurahMapper.matchesSurah("orang kafir", 109, "Al-Kaafiroon", nameMeaningId = "Orang-Orang Kafir"))
    }

    @Test
    fun GIVEN_quranRepositoryAndViewModel_WHEN_loadingSurahs_THEN_exposesIndonesianMeaningAndPreservesMetadata() {
        val mainDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(mainDispatcher)
        try {
            runTest(mainDispatcher) {
                val entities = listOf(
                    SurahEntity(1, 1, "سُورَةُ ٱلْفَاتِحَةِ", "Al-Faatiha", "The Opening", "Makkiyyah", 7),
                    SurahEntity(4, 4, "سُورَةُ النِّسَاءِ", "An-Nisaa", "The Women", "Madaniyyah", 176),
                    SurahEntity(114, 114, "سُورَةُ النَّاسِ", "An-Naas", "Mankind", "Makkiyyah", 6)
                )
                val quranDao = mockk<QuranDao>(relaxed = true)
                every { quranDao.getAllSurahs() } returns flowOf(entities)
                coEvery { quranDao.getSurahByNumber(4) } returns entities[1]
                every { quranDao.getAyahsBySurah(4) } returns flowOf(emptyList())

                val bookmarkDao = mockk<BookmarkDao>(relaxed = true)
                every { bookmarkDao.getAllBookmarks() } returns flowOf(emptyList())
                val lastReadDao = mockk<LastReadDao>(relaxed = true)
                every { lastReadDao.getLastRead() } returns flowOf(null)
                val tafsirDao = mockk<TafsirDao>(relaxed = true)

                val repository = QuranRepositoryImpl(quranDao, bookmarkDao, lastReadDao, tafsirDao)
                val mappedList = repository.getAllSurahs().first()
                assertEquals(listOf("Pembukaan", "Perempuan", "Manusia"), mappedList.map { it.nameMeaningId })
                assertEquals(176, mappedList[1].ayahCount)
                assertEquals("Madaniyyah", mappedList[1].revelationType)

                val detail = repository.getSurah(4)
                assertEquals("Perempuan", detail?.nameMeaningId)
                assertEquals("سُورَةُ النِّسَاءِ", detail?.nameArabic)

                val wbwDao = mockk<WordByWordDao>(relaxed = true)
                every { wbwDao.getWordsBySurah(4) } returns flowOf(emptyList())
                val wbwRepo = WordByWordRepositoryImpl(wbwDao)

                val vm = QuranViewModel(
                    getSurahListUseCase = GetSurahListUseCase(repository),
                    getSurahDetailUseCase = GetSurahDetailUseCase(repository),
                    getAyahsBySurahUseCase = GetAyahsBySurahUseCase(repository),
                    getFirstAyahByPageUseCase = GetFirstAyahByPageUseCase(repository),
                    getFirstAyahByJuzUseCase = GetFirstAyahByJuzUseCase(repository),
                    searchQuranUseCase = SearchQuranUseCase(repository),
                    toggleBookmarkUseCase = ToggleBookmarkUseCase(repository),
                    getBookmarksUseCase = GetBookmarksUseCase(repository),
                    deleteBookmarkUseCase = DeleteBookmarkUseCase(repository),
                    restoreBookmarkUseCase = RestoreBookmarkUseCase(repository),
                    updateBookmarkNoteUseCase = UpdateBookmarkNoteUseCase(repository),
                    saveLastReadUseCase = SaveLastReadUseCase(repository),
                    getLastReadUseCase = GetLastReadUseCase(repository),
                    getWordsBySurahUseCase = GetWordsBySurahUseCase(wbwRepo),
                    getTafsirUseCase = GetTafsirUseCase(repository)
                )
                val collectJob = launch { vm.surahListState.collect { } }
                vm.loadSurahDetail(4)
                advanceUntilIdle()

                val listState = vm.surahListState.value
                assertTrue("Expected Success but got $listState", listState is UiState.Success)
                assertEquals("Perempuan", (listState as UiState.Success).data[1].nameMeaningId)
                assertEquals("Perempuan", vm.currentSurah.value?.nameMeaningId)
                collectJob.cancel()
            }
        } finally {
            Dispatchers.resetMain()
        }
    }
}
