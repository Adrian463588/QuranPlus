package com.quranplus.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
        composeRule.onNodeWithText("Model lokal belum tersedia").assertIsDisplayed()
        composeRule.onNodeWithText("ModelGate diblokir: MODEL_UNAVAILABLE, EMBEDDER_UNAVAILABLE, INDEX_UNAVAILABLE. Katalog, embedding, dan index harus memiliki provenance serta SHA-256 yang direview.")
            .assertIsDisplayed()
    }

    @Test
    fun GIVEN_quranHome_WHEN_settingsDestinationIsClicked_THEN_settingsActionsAreDisplayed() {
        waitForText("Al-Qur'an Al-Karim")

        composeRule.onNodeWithContentDescription("Menu Al-Qur'an").performClick()
        composeRule.onNodeWithText("Pengaturan").performClick()

        waitForText("Fitur Tajwid & Murottal")
        composeRule.onNodeWithText("Folder Model & RAG (SAF)").assertIsDisplayed()
    }

    @Test
    fun GIVEN_quranHome_WHEN_bookmarkDestinationIsClicked_THEN_bookmarkScreenIsDisplayed() {
        waitForText("Al-Qur'an Al-Karim")

        composeRule.onNodeWithText("Bookmark").performClick()

        waitForText("Daftar Bookmark")
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText(text, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }
}
