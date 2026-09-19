package com.quranplus.app

import com.quranplus.app.features.rag.domain.CitationMarkerValidator
import com.quranplus.app.features.rag.domain.RetrievedCitation
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CitationMarkerValidatorTest {
    @Test
    fun GIVEN_retrievedCitationId_WHEN_modelUsesIt_THEN_answerIsValid() {
        val result = CitationMarkerValidator.validate(
            "Talak dibahas pada sumber. [[cite:C1]]",
            listOf(citation())
        )

        assertTrue(result.isValid)
        assertTrue(result.invalidIds.isEmpty())
    }

    @Test
    fun GIVEN_unknownCitationId_WHEN_modelUsesIt_THEN_answerIsRejectedAndMarkerRemoved() {
        val result = CitationMarkerValidator.validate(
            "Klaim. [[cite:C99]]",
            listOf(citation())
        )

        assertFalse(result.isValid)
        assertTrue(result.content.contains("Klaim."))
        assertFalse(result.content.contains("C99"))
    }

    @Test
    fun GIVEN_noCitationMarker_WHEN_answerHasRetrievedContext_THEN_answerIsRejected() {
        val result = CitationMarkerValidator.validate("Klaim tanpa sumber", listOf(citation()))

        assertFalse(result.isValid)
    }

    private fun citation() = RetrievedCitation(
        sourceId = "quran-2-227",
        sourceType = "quran",
        title = "QS. Al-Baqarah: 227",
        reference = "QS. Al-Baqarah: 227",
        textSnippet = "Isi ayat",
        score = 0.9f,
        surahNumber = 2,
        ayahNumber = 227,
        deepLinkTarget = "quran:2:227"
    )
}
