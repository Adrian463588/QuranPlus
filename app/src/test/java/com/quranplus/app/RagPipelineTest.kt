package com.quranplus.app

import com.quranplus.app.features.rag.domain.RagPipeline
import com.quranplus.app.features.rag.domain.RetrievedCitation
import com.quranplus.app.features.settings.data.AiPersona
import org.junit.Assert.assertTrue
import org.junit.Test

class RagPipelineTest {

    @Test
    fun GIVEN_questionAndCitations_WHEN_buildAugmentedPrompt_THEN_containsGroundTruthAndPersona() {
        val pipeline = RagPipeline()

        val citations = listOf(
            RetrievedCitation(
                sourceId = "surah_2_183",
                sourceType = "quran",
                title = "QS. Al-Baqarah: 183",
                reference = "QS. Al-Baqarah: 183",
                textSnippet = "Hai orang-orang yang beriman, diwajibkan atas kamu berpuasa sebagaimana diwajibkan atas orang-orang sebelum kamu...",
                score = 0.92f,
                surahNumber = 2,
                ayahNumber = 183
            )
        )

        val prompt = pipeline.buildAugmentedPrompt(
            question = "Apa hukum puasa Ramadhan?",
            persona = AiPersona.USTADZ,
            customPrompt = null,
            citations = citations
        )

        assertTrue(prompt.contains("QS. Al-Baqarah: 183"))
        assertTrue(prompt.contains("Apa hukum puasa Ramadhan?"))
        assertTrue(prompt.contains("Ustadz"))
    }
}
