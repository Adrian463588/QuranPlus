package com.quranplus.app

import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quranplus.app.core.database.QuranDatabase
import com.quranplus.app.core.ui.components.AdaptiveNavigationScaffold
import com.quranplus.app.core.ui.theme.QuranPlusTheme
import com.quranplus.app.core.utils.SurahMapper
import com.quranplus.app.features.quran.data.WordByWordRepositoryImpl
import com.quranplus.app.features.quran.domain.Ayah
import com.quranplus.app.features.quran.domain.Bookmark
import com.quranplus.app.features.quran.domain.BookmarkSort
import com.quranplus.app.features.quran.domain.DeleteBookmarkUseCase
import com.quranplus.app.features.quran.domain.GetAyahsBySurahUseCase
import com.quranplus.app.features.quran.domain.GetBookmarksUseCase
import com.quranplus.app.features.quran.domain.GetFirstAyahByJuzUseCase
import com.quranplus.app.features.quran.domain.GetFirstAyahByPageUseCase
import com.quranplus.app.features.quran.domain.GetLastReadUseCase
import com.quranplus.app.features.quran.domain.GetSurahDetailUseCase
import com.quranplus.app.features.quran.domain.GetSurahListUseCase
import com.quranplus.app.features.quran.domain.GetTafsirUseCase
import com.quranplus.app.features.quran.domain.GetWordsBySurahUseCase
import com.quranplus.app.features.quran.domain.LastRead
import com.quranplus.app.features.quran.domain.QuranRepository
import com.quranplus.app.features.quran.domain.QuranSearchFilter
import com.quranplus.app.features.quran.domain.RestoreBookmarkUseCase
import com.quranplus.app.features.quran.domain.SaveLastReadUseCase
import com.quranplus.app.features.quran.domain.SearchQuranUseCase
import com.quranplus.app.features.quran.domain.Surah
import com.quranplus.app.features.quran.domain.Tafsir
import com.quranplus.app.features.quran.domain.ToggleBookmarkUseCase
import com.quranplus.app.features.quran.domain.UpdateBookmarkNoteUseCase
import com.quranplus.app.features.quran.presentation.QuranViewModel
import com.quranplus.app.features.quran.presentation.SurahListScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdaptiveNavigationSemanticsTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun GIVEN_compactWindow_WHEN_navigationRenders_THEN_bottomDestinationsAreVisible() {
        render(WindowWidthSizeClass.Compact)

        composeRule.onNodeWithText("Al-Qur'an").assertIsDisplayed()
        composeRule.onNodeWithText("Hadist").assertIsDisplayed()
        composeRule.onNodeWithText("Tanya AI").assertIsDisplayed()
        composeRule.onNodeWithText("Tahsin").assertIsDisplayed()
        composeRule.onNodeWithText("More").assertIsDisplayed()
    }

    @Test
    fun GIVEN_mediumWindow_WHEN_navigationRenders_THEN_railDestinationsAreVisible() {
        render(WindowWidthSizeClass.Medium)

        composeRule.onNodeWithText("Al-Qur'an").assertIsDisplayed()
        composeRule.onNodeWithText("Tahsin").assertIsDisplayed()
        composeRule.onNodeWithText("Bookmark").performScrollTo()
        composeRule.onNodeWithText("Bookmark").assertIsDisplayed()
    }

    @Test
    fun GIVEN_expandedWindow_WHEN_navigationRenders_THEN_drawerDestinationsAreVisible() {
        render(WindowWidthSizeClass.Expanded)

        composeRule.onNodeWithText("Quran Plus").assertIsDisplayed()
        composeRule.onNodeWithText("Al-Qur'an").assertIsDisplayed()
        composeRule.onNodeWithText("Pengaturan").assertIsDisplayed()
    }

    private fun render(widthSizeClass: WindowWidthSizeClass) {
        composeRule.activity.runOnUiThread {
            composeRule.activity.setContent {
                QuranPlusTheme {
                    AdaptiveNavigationScaffold(
                        currentRoute = "quran_home",
                        widthSizeClass = widthSizeClass,
                        onNavigateToDestination = {},
                        content = { Text("Konten Quran") }
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun GIVEN_compactAndExpandedSurahScreenWithLargeFont_WHEN_renderingAndSelecting_THEN_meaningsReadableAndClickable() {
        val db = QuranDatabase.getInstance(composeRule.activity)
        val surahs = SurahMapper.getAllSurahs().map { ref ->
            Surah(
                number = ref.number,
                nameArabic = if (ref.number == 4) "سُورَةُ النِّسَاءِ" else "سُورَةُ",
                nameLatin = ref.latinName,
                nameEnglish = "",
                revelationType = if (ref.number in listOf(2, 3, 4, 5)) "Madaniyyah" else "Makkiyyah",
                ayahCount = ref.ayahCount,
                nameMeaningId = ref.meaningId
            )
        }
        val repo = object : QuranRepository {
            override fun getAllSurahs(): Flow<List<Surah>> = flowOf(surahs)
            override suspend fun getSurah(surahNumber: Int): Surah? = surahs.getOrNull(surahNumber - 1)
            override fun getAyahsBySurah(surahNumber: Int): Flow<List<Ayah>> = flowOf(emptyList())
            override suspend fun getFirstAyahByPage(page: Int): Ayah? = null
            override suspend fun getFirstAyahByJuz(juz: Int): Ayah? = null
            override suspend fun searchAyahs(query: String, filter: QuranSearchFilter): List<Ayah> = emptyList()
            override fun getAllBookmarks(sort: BookmarkSort): Flow<List<Bookmark>> = flowOf(emptyList())
            override fun isAyahBookmarked(surahNumber: Int, ayahNumber: Int): Flow<Boolean> = flowOf(false)
            override suspend fun toggleBookmark(surahNumber: Int, surahName: String, ayahNumber: Int, textArabic: String, translation: String, note: String?) {}
            override suspend fun deleteBookmark(id: Long) {}
            override suspend fun restoreBookmark(bookmark: Bookmark) {}
            override suspend fun updateBookmarkNote(id: Long, note: String?) {}
            override fun getLastRead(): Flow<LastRead?> = flowOf(null)
            override suspend fun saveLastRead(surahNumber: Int, surahName: String, ayahNumber: Int, juz: Int, page: Int) {}
            override suspend fun getTafsir(surahNumber: Int, ayahNumber: Int): Tafsir? = null
            override fun getTafsirsForSurah(surahNumber: Int): Flow<List<Tafsir>> = flowOf(emptyList())
            override fun getSearchHistory(): Flow<List<String>> = flowOf(emptyList())
            override suspend fun saveSearchQuery(query: String) {}
            override suspend fun deleteSearchQuery(query: String) {}
            override suspend fun clearSearchHistory() {}
        }
        val wbwRepo = WordByWordRepositoryImpl(db.wordByWordDao())
        val vm = QuranViewModel(
            GetSurahListUseCase(repo),
            GetSurahDetailUseCase(repo),
            GetAyahsBySurahUseCase(repo),
            GetFirstAyahByPageUseCase(repo),
            GetFirstAyahByJuzUseCase(repo),
            SearchQuranUseCase(repo),
            ToggleBookmarkUseCase(repo),
            GetBookmarksUseCase(repo),
            DeleteBookmarkUseCase(repo),
            RestoreBookmarkUseCase(repo),
            UpdateBookmarkNoteUseCase(repo),
            SaveLastReadUseCase(repo),
            GetLastReadUseCase(repo),
            GetWordsBySurahUseCase(wbwRepo),
            GetTafsirUseCase(repo)
        )
        var openedSurah = -1
        composeRule.activity.runOnUiThread {
            composeRule.activity.setContent {
                val baseDensity = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(baseDensity.density, fontScale = 2.0f)) {
                    QuranPlusTheme {
                        SurahListScreen(
                            viewModel = vm,
                            onSurahClick = { surah, _ -> openedSurah = surah },
                            onSearchClick = {},
                            onNavigateToSettings = {},
                            widthSizeClass = WindowWidthSizeClass.Expanded
                        )
                    }
                }
            }
        }
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("Pembukaan", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithText("Pembukaan", useUnmergedTree = true).onFirst().assertIsDisplayed()
        composeRule.onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToNode(hasText("An-Nisa'"))
        composeRule.onAllNodes(hasText("An-Nisa'") and hasClickAction()).onFirst().performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("Perempuan", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(
            composeRule.onAllNodesWithText(
                "Baca surah ini dengan tampilan reader yang mendukung tajwid, terjemahan, transliterasi, dan bookmark.",
                useUnmergedTree = true
            ).fetchSemanticsNodes().isEmpty()
        )
        composeRule.onNodeWithText("Buka reader").performClick()
        assertEquals(4, openedSurah)

        // Compact phone layout under 2.0x accessibility font scale
        openedSurah = -1
        composeRule.activity.runOnUiThread {
            composeRule.activity.setContent {
                val baseDensity = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(baseDensity.density, fontScale = 2.0f)) {
                    QuranPlusTheme {
                        SurahListScreen(
                            viewModel = vm,
                            onSurahClick = { surah, _ -> openedSurah = surah },
                            onSearchClick = {},
                            onNavigateToSettings = {},
                            widthSizeClass = WindowWidthSizeClass.Compact
                        )
                    }
                }
            }
        }
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("Pembukaan", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithText("Pembukaan", useUnmergedTree = true).onFirst().assertIsDisplayed()
        composeRule.onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToNode(hasText("An-Nisa'"))
        composeRule.onAllNodes(hasText("An-Nisa'") and hasClickAction()).onFirst().performClick()
        assertEquals(4, openedSurah)
    }
}
