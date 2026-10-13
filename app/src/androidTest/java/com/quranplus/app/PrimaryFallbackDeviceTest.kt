package com.quranplus.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.quranplus.app.features.chatbot.data.InternetResearcher
import com.quranplus.app.features.rag.domain.isPrimaryEvidence
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Live acceptance; requires validated internet and the real Quran provider. */
@RunWith(AndroidJUnit4::class)
class PrimaryFallbackDeviceTest {
    @Test
    fun GIVEN_validatedInternet_WHEN_searchingPuasa_THEN_returnsActualPrimaryQuran() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val citations = InternetResearcher(context).search("puasa")
        assertTrue("Live primary provider is unavailable", citations.isNotEmpty())
        assertTrue(citations.all { it.isPrimaryEvidence() })
        assertTrue(citations.any { it.surahNumber == 2 && it.ayahNumber == 183 })
    }
}
