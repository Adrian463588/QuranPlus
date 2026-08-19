package com.quranplus.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainNavigationSmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun GIVEN_quranHome_WHEN_searchActionIsClicked_THEN_searchScreenIsDisplayed() {
        waitForText("Al-Qur'an Al-Karim")

        composeRule.onNodeWithContentDescription("Menu Al-Qur'an").performClick()
        composeRule.onNodeWithText("Cari ayat").performClick()

        waitForText("Pencarian FTS5 Cepat")
        composeRule.onNodeWithText("Filter").assertIsDisplayed()
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
    }

    @Test
    fun GIVEN_quranHome_WHEN_chatDestinationIsClicked_THEN_modelGateIsDisplayed() {
        waitForText("Al-Qur'an Al-Karim")

        composeRule.onNodeWithText("Tanya AI").performClick()

        waitForText("Setup AI On-Device")
        composeRule.onNodeWithText("Pilih Model AI yang Diinginkan:").assertIsDisplayed()
        composeRule.onNodeWithText("Gemma 4 E2B IT (LiteRT-LM)").assertIsDisplayed()
        composeRule.onNodeWithText("Gemma 3 1B IT").assertIsDisplayed()
        composeRule.onAllNodesWithText("Buka sumber model").assertCountEquals(2)
        composeRule.onAllNodesWithText("Buka link unduh").assertCountEquals(2)
        composeRule.onNodeWithTag("model_catalog").performTouchInput { swipeUp() }
        composeRule.onNodeWithText("Qwen 2.5 1.5B Instruct").assertIsDisplayed()
        composeRule.onAllNodesWithText("Buka sumber model").assertCountEquals(2)
        composeRule.onNodeWithText("Model diblokir sampai manifest SHA-256 terverifikasi tersedia.")
            .assertIsDisplayed()
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
            composeRule.onAllNodesWithText(text, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private fun waitForContentDescription(description: String) {
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithContentDescription(description, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }
}
