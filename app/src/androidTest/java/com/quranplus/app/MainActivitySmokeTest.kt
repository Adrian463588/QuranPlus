package com.quranplus.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import org.junit.Rule
import org.junit.Test

class MainActivitySmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun GIVEN_fresh_launch_WHEN_contentSettles_THEN_rootIsDisplayed() {
        composeRule.waitForIdle()
        composeRule.onRoot().assertIsDisplayed()
    }
}
