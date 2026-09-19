package com.quranplus.app

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quranplus.app.core.ui.theme.QuranPlusTheme
import com.quranplus.app.features.chatbot.presentation.CitationChip
import com.quranplus.app.features.rag.domain.RetrievedCitation
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CitationChipNavigationTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun GIVEN_quranCitation_WHEN_chipIsTapped_THEN_callbackRuns() {
        var clicked = false
        render(
            citation = RetrievedCitation(
                sourceId = "quran-2-229",
                sourceType = "quran",
                title = "QS. Al-Baqarah:229",
                reference = "QS. Al-Baqarah:229",
                textSnippet = "Talak yang dapat dirujuki dua kali.",
                score = 0.95f,
                surahNumber = 2,
                ayahNumber = 229
            ),
            onClick = { clicked = true }
        )

        composeRule.onNodeWithContentDescription("Buka QS. Al-Baqarah:229").performClick()
        composeRule.runOnIdle { assertTrue(clicked) }
    }

    @Test
    fun GIVEN_hadithCitation_WHEN_chipIsTapped_THEN_callbackRuns() {
        var clicked = false
        render(
            citation = RetrievedCitation(
                sourceId = "hadith-1",
                sourceType = "hadith",
                title = "Sahih al-Bukhari No. 1",
                reference = "Sahih al-Bukhari no. 1",
                textSnippet = "Amal bergantung pada niat.",
                score = 0.95f,
                collection = "bukhari",
                identifier = "1"
            ),
            onClick = { clicked = true }
        )

        composeRule.onNodeWithContentDescription("Buka Sahih al-Bukhari No. 1").performClick()
        composeRule.runOnIdle { assertTrue(clicked) }
    }

    @Test
    fun GIVEN_savedCitationWithDeepLink_WHEN_chipIsTapped_THEN_callbackRuns() {
        var clicked = false
        render(
            citation = RetrievedCitation(
                sourceId = "legacy-quran-reference",
                sourceType = "quran",
                title = "QS. Al-Baqarah:229",
                reference = "QS. Al-Baqarah:229",
                textSnippet = "Talak yang dapat dirujuki dua kali.",
                score = 0.95f,
                deepLinkTarget = "quran:2:229"
            ),
            onClick = { clicked = true }
        )

        composeRule.onNodeWithContentDescription("Buka QS. Al-Baqarah:229").performClick()
        composeRule.runOnIdle { assertTrue(clicked) }
    }

    @Test
    fun GIVEN_webCitation_WHEN_chipIsTapped_THEN_callbackRuns() {
        var clicked = false
        render(
            citation = RetrievedCitation(
                sourceId = "web:id.wikipedia.org:123",
                sourceType = "internet",
                title = "Internet • Sejarah Islam",
                reference = "id.wikipedia.org — Sejarah Islam",
                textSnippet = "Ringkasan dari halaman web.",
                score = 0.74f,
                deepLinkTarget = "https://id.wikipedia.org/wiki/Sejarah_Islam"
            ),
            onClick = { clicked = true }
        )

        composeRule
            .onNodeWithContentDescription("Buka Internet • Sejarah Islam")
            .performClick()
        composeRule.runOnIdle { assertTrue(clicked) }
    }

    private fun render(citation: RetrievedCitation, onClick: () -> Unit) {
        composeRule.setContent {
            QuranPlusTheme {
                CitationChip(citation = citation, onClick = onClick)
            }
        }
        composeRule.waitForIdle()
    }
}
