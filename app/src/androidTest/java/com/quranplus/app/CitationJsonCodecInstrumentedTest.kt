package com.quranplus.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quranplus.app.features.chatbot.data.CitationJsonCodec
import com.quranplus.app.features.rag.domain.RetrievedCitation
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CitationJsonCodecInstrumentedTest {

    @Test
    fun GIVEN_citationWithDeepLink_WHEN_roundTripped_THEN_exactEvidenceIsPreserved() {
        val citation = RetrievedCitation(
            sourceId = "quran:2:255",
            sourceType = "quran",
            title = "QS. Al-Baqarah: 255",
            reference = "QS. Al-Baqarah: 255",
            textSnippet = "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ",
            score = 0.8125f,
            surahNumber = 2,
            ayahNumber = 255
        )

        val restored = CitationJsonCodec.decode(CitationJsonCodec.encode(listOf(citation))).single()

        assertEquals(citation, restored)
    }
}
