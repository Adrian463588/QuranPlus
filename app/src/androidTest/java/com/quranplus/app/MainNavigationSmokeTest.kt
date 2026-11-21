package com.quranplus.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainNavigationSmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun resetNavigationToQuranHome() {
        waitForText("Al-Qur'an")
        composeRule.onAllNodesWithText("Al-Qur'an")[0].performClick()
    }

    @Test
    fun GIVEN_quranHome_WHEN_searchActionIsClicked_THEN_searchScreenIsDisplayed() {
        waitForText("Al-Qur'an Al-Karim")

        composeRule.onNodeWithContentDescription("Menu Al-Qur'an").performClick()
        composeRule.onNodeWithText("Cari ayat").performClick()

        waitForText("Pencarian FTS5 Cepat")
        composeRule.onNodeWithText("Semua Surah").assertIsDisplayed()
    }

    @Test
    fun GIVEN_searchScreen_WHEN_specificFilterIsOpened_THEN_sourceAndPhraseOptionsAreDisplayed() {
        waitForText("Al-Qur'an Al-Karim")

        composeRule.onNodeWithContentDescription("Menu Al-Qur'an").performClick()
        composeRule.onNodeWithText("Cari ayat").performClick()
        composeRule.onNodeWithTag("search_filter_specific").performClick()

        waitForText("Pencarian spesifik")
        composeRule.onNodeWithText("Transliterasi Latin").assertIsDisplayed()
        composeRule.onNodeWithText("Frasa tepat").assertIsDisplayed()
    }

    @Test
    fun GIVEN_quranHome_WHEN_tahsinDestinationIsClicked_THEN_tahsinScreenIsDisplayed() {
        waitForText("Al-Qur'an Al-Karim")

        composeRule.onNodeWithText("Tahsin").performClick()

        waitForText("Tahsin & Makharij")
    }

    @Test
    fun GIVEN_quranHome_WHEN_hadithDestinationIsClicked_THEN_hadithScreenIsDisplayed() {
        waitForText("Al-Qur'an Al-Karim")

        composeRule.onNodeWithText("Hadist").performClick()

        waitForText("Hadist")
        val hasCatalog = waitForAnyText("Kutubus Sittah", "Bundle Hadist offline")
        assertTrue(hasCatalog)
        if (nodeHasText("Kutubus Sittah")) {
            waitForText("Sahih al-Bukhari")
            composeRule.onNodeWithTag("hadith_collection_catalog")
                .performScrollToNode(hasText("Hadis Lainnya"))
            waitForText("Hadis Lainnya")
        }
    }

    @Test
    fun GIVEN_quranHome_WHEN_chatDestinationIsClicked_THEN_modelGateIsDisplayed() {
        waitForText("Al-Qur'an Al-Karim")

        composeRule.onNodeWithText("Tanya AI").performClick()

        waitForText("Tanya AI On-Device")
        composeRule.onNodeWithText("Pilih Model AI").assertIsDisplayed()
        composeRule.onNodeWithTag("model_catalog")
            .performScrollToNode(hasText("Gemma 4 E2B IT"))
        composeRule.onNodeWithText("Gemma 4 E2B IT").assertIsDisplayed()
        composeRule.onNodeWithTag("model_catalog")
            .performScrollToNode(hasText("Model Embedding RAG"))
        composeRule.onNodeWithText("Model Embedding RAG").assertIsDisplayed()
    }

    @Test
    fun GIVEN_quranHome_WHEN_settingsDestinationIsClicked_THEN_settingsActionsAreDisplayed() {
        waitForText("Al-Qur'an Al-Karim")

        composeRule.onNodeWithText("More").performClick()
        composeRule.onAllNodes(hasText("Pengaturan") and hasClickAction()).onFirst().performClick()

        waitForText("Tajwid Berwarna (High Contrast)")
        composeRule.onNodeWithText("Folder Model & Dokumen (SAF)").performScrollTo()
        composeRule.onNodeWithText("Folder Model & Dokumen (SAF)").assertIsDisplayed()
    }

    @Test
    fun GIVEN_quranHome_WHEN_bookmarkDestinationIsClicked_THEN_bookmarkScreenIsDisplayed() {
        waitForText("Al-Qur'an Al-Karim")

        composeRule.onNodeWithText("More").performClick()
        composeRule.onAllNodes(hasText("Bookmark") and hasClickAction()).onFirst().performClick()

        waitForText("Daftar Bookmark")
    }

    @Test
    fun GIVEN_reader_WHEN_titleIdentityIsClicked_THEN_quranRootIsDisplayed() {
        waitForText("Al-Qur'an Al-Karim")

        composeRule.onAllNodesWithText("Al-Qur'an")[0].performClick()
        openFirstSurahReader()
        waitForContentDescription("Beranda Quran")
        composeRule.onNodeWithContentDescription("Beranda Quran").performClick()

        waitForText("Al-Qur'an Al-Karim")
    }

    @Test
    fun GIVEN_reader_WHEN_hamburgerHomeIsClicked_THEN_quranRootIsDisplayed() {
        waitForText("Al-Qur'an Al-Karim")

        composeRule.onAllNodesWithText("Al-Qur'an")[0].performClick()
        openFirstSurahReader()
        waitForContentDescription("Menu Quran")
        composeRule.onNodeWithContentDescription("Menu Quran").performClick()
        composeRule.onNodeWithText("Beranda Quran").performClick()

        waitForText("Al-Qur'an Al-Karim")
    }

    private fun openFirstSurahReader() {
        waitForText("Al-Faatiha")
        composeRule.onAllNodes(hasText("Al-Faatiha") and hasClickAction()).onFirst().performClick()
        val hasOpenButton = runCatching {
            composeRule.onAllNodesWithText("Buka reader").fetchSemanticsNodes().isNotEmpty()
        }.getOrDefault(false)
        if (hasOpenButton) {
            composeRule.onNodeWithText("Buka reader").performClick()
        }
    }

    @Test
    fun GIVEN_quranHomeAndSearch_WHEN_browsingSelectingScrollingAndSearchingMeanings_THEN_displaysAllIndonesianMeaningsCorrectly() {
        waitForText("Al-Faatiha")
        composeRule.onAllNodesWithText("Pembukaan", useUnmergedTree = true).fetchSemanticsNodes().let {
            assertTrue("Expected Pembukaan in Surah list", it.isNotEmpty())
        }
        composeRule.onAllNodesWithText("Sapi Betina", useUnmergedTree = true).fetchSemanticsNodes().let {
            assertTrue("Expected Sapi Betina in Surah list", it.isNotEmpty())
        }
        composeRule.onAllNodesWithText("Perempuan", useUnmergedTree = true).fetchSemanticsNodes().let {
            assertTrue("Expected Perempuan for An-Nisaa in Surah list", it.isNotEmpty())
        }

        // Open/select An-Nisaa cleanly across both Compact (reader navigation) and Expanded (detail panel)
        composeRule.onAllNodes(hasText("An-Nisaa") and hasClickAction()).onFirst().performClick()
        assertTrue(
            composeRule.onAllNodesWithText(
                "Baca surah ini dengan tampilan reader yang mendukung tajwid, terjemahan, transliterasi, dan bookmark.",
                useUnmergedTree = true
            ).fetchSemanticsNodes().isEmpty()
        )
        val hasOpenButton = runCatching {
            composeRule.onAllNodesWithText("Buka reader").fetchSemanticsNodes().isNotEmpty()
        }.getOrDefault(false)
        if (hasOpenButton) {
            composeRule.onAllNodesWithText("Perempuan", useUnmergedTree = true).fetchSemanticsNodes().let {
                assertTrue("Expected Perempuan in list and detail panel", it.size <= 2)
            }
            composeRule.onNodeWithText("Buka reader").performClick()
        }
        waitForText("An-Nisaa")
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }

        // Scroll to Surah 114 and verify An-Naas -> Manusia
        waitForText("Al-Faatiha")
        val listNode = composeRule.onAllNodes(androidx.compose.ui.test.hasScrollToNodeAction()).onFirst()
        listNode.performScrollToNode(hasText("An-Naas"))
        waitForText("Manusia")

        // Search by Indonesian Surah meaning ("Sapi Betina") -> Al-Baqara without At-Tin
        composeRule.onNodeWithContentDescription("Cari ayat").performClick()
        waitForText("Pencarian FTS5 Cepat")
        composeRule.onAllNodes(androidx.compose.ui.test.hasSetTextAction()).onFirst().performClick()
        composeRule.onAllNodes(androidx.compose.ui.test.hasSetTextAction()).onFirst().performTextInput("Sapi Betina")
        waitForText("Al-Baqara")
        assertTrue(
            composeRule.onAllNodesWithText("At-Tin", useUnmergedTree = true).fetchSemanticsNodes().isEmpty()
        )
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(10_000) {
            runCatching {
                composeRule.onAllNodesWithText(text, useUnmergedTree = true)
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }.getOrDefault(false)
        }
    }

    private fun waitForContentDescription(description: String) {
        composeRule.waitUntil(10_000) {
            runCatching {
                composeRule.onAllNodesWithContentDescription(description, useUnmergedTree = true)
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }.getOrDefault(false)
        }
    }

    private fun waitForAnyText(vararg texts: String): Boolean {
        composeRule.waitUntil(10_000) {
            texts.any(::nodeHasText)
        }
        return texts.any(::nodeHasText)
    }

    private fun nodeHasText(text: String): Boolean = runCatching {
        composeRule.onAllNodesWithText(text, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()
    }.getOrDefault(false)
}
