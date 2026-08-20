package com.quranplus.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performClick
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
        composeRule.onNodeWithText("Filter").assertIsDisplayed()
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
        waitForText("Bundle Hadist offline")
        val hasCatalog = waitForAnyText("Kutubus Sittah", "Belum ada katalog hadist yang dimuat.")
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

        waitForText("Setup AI On-Device")
        composeRule.onNodeWithText("Pilih Model AI yang Diinginkan:").assertIsDisplayed()
        composeRule.onNodeWithTag("model_catalog")
            .performScrollToNode(hasText("Gemma 3 1B IT"))
        composeRule.onNodeWithText("Gemma 3 1B IT").assertIsDisplayed()
        assertTrue(
            composeRule.onAllNodesWithText("Buka sumber model")
                .fetchSemanticsNodes().isNotEmpty()
        )
        assertTrue(
            composeRule.onAllNodesWithText("Buka link unduh")
                .fetchSemanticsNodes().isNotEmpty()
        )
        composeRule.onNodeWithTag("model_catalog")
            .performScrollToNode(hasText("Qwen 2.5 1.5B Instruct"))
        composeRule.onNodeWithText("Qwen 2.5 1.5B Instruct").assertIsDisplayed()
        assertTrue(
            composeRule.onAllNodesWithText("Siap diunduh dan diverifikasi.")
                .fetchSemanticsNodes().isNotEmpty()
        )
        composeRule.onNodeWithTag("model_catalog")
            .performScrollToNode(hasText("Gemma 4 E2B IT"))
        composeRule.onNodeWithText("Gemma 4 E2B IT").assertIsDisplayed()
        assertTrue(
            composeRule.onAllNodesWithText("Siap diunduh dan diverifikasi.")
                .fetchSemanticsNodes().isNotEmpty()
        )
    }

    @Test
    fun GIVEN_quranHome_WHEN_settingsDestinationIsClicked_THEN_settingsActionsAreDisplayed() {
        waitForText("Al-Qur'an Al-Karim")

        composeRule.onNodeWithText("More").performClick()
        composeRule.onNodeWithText("Pengaturan").performClick()

        waitForText("Fitur Tajwid & Murottal")
        composeRule.onNodeWithText("Folder Model & RAG (SAF)").assertIsDisplayed()
    }

    @Test
    fun GIVEN_quranHome_WHEN_bookmarkDestinationIsClicked_THEN_bookmarkScreenIsDisplayed() {
        waitForText("Al-Qur'an Al-Karim")

        composeRule.onNodeWithText("More").performClick()
        composeRule.onNodeWithText("Bookmark").performClick()

        waitForText("Daftar Bookmark")
    }

    @Test
    fun GIVEN_reader_WHEN_titleIdentityIsClicked_THEN_quranRootIsDisplayed() {
        waitForText("Al-Qur'an Al-Karim")

        composeRule.onNodeWithText("Al-Qur'an").performClick()
        waitForText("Al-Faatiha")
        composeRule.onNodeWithText("Al-Faatiha").performClick()
        waitForContentDescription("Beranda Quran")
        composeRule.onNodeWithContentDescription("Beranda Quran").performClick()

        waitForText("Al-Qur'an Al-Karim")
    }

    @Test
    fun GIVEN_reader_WHEN_hamburgerHomeIsClicked_THEN_quranRootIsDisplayed() {
        waitForText("Al-Qur'an Al-Karim")

        composeRule.onNodeWithText("Al-Qur'an").performClick()
        waitForText("Al-Faatiha")
        composeRule.onNodeWithText("Al-Faatiha").performClick()
        waitForContentDescription("Menu Quran")
        composeRule.onNodeWithContentDescription("Menu Quran").performClick()
        composeRule.onNodeWithText("Beranda Quran").performClick()

        waitForText("Al-Qur'an Al-Karim")
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
