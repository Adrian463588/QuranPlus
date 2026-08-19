package com.quranplus.app

import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quranplus.app.core.ui.components.AdaptiveNavigationScaffold
import com.quranplus.app.core.ui.theme.QuranPlusTheme
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
        composeRule.onNodeWithText("Bookmark").assertIsDisplayed()
    }

    @Test
    fun GIVEN_mediumWindow_WHEN_navigationRenders_THEN_railDestinationsAreVisible() {
        render(WindowWidthSizeClass.Medium)

        composeRule.onNodeWithText("Al-Qur'an").assertIsDisplayed()
        composeRule.onNodeWithText("Tahsin").assertIsDisplayed()
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
}
