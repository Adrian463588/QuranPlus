package com.quranplus.app

import com.quranplus.app.features.rag.domain.EvidenceKind
import com.quranplus.app.features.rag.domain.RetrievedCitation
import com.quranplus.app.features.rag.domain.isPrimaryEvidence
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrimaryEvidenceTest {
    @Test
    fun GIVEN_localPrimaryTexts_WHEN_validating_THEN_acceptsExactSourceTargets() {
        assertTrue(quran().isPrimaryEvidence())
        assertTrue(quran().copy(
            sourceType = "hadith", evidenceKind = EvidenceKind.HADITH,
            deepLinkTarget = "hadith:bukhari:1"
        ).isPrimaryEvidence())
    }

    @Test
    fun GIVEN_spoofedOnlineQuranOrMetadata_WHEN_validating_THEN_rejectsIt() {
        listOf(
            quran().copy(deepLinkTarget = "https://islam.nu.or.id/article"),
            quran().copy(providerId = "alquran-cloud", deepLinkTarget = "https://sunnah.com/bukhari:1"),
            quran().copy(evidenceKind = EvidenceKind.WEB),
            quran().copy(sourceType = "user_document"),
            quran().copy(score = Float.NaN),
            quran().copy(deepLinkTarget = null, sourceId = "unknown"),
            quran().copy(textSnippet = "")
        ).forEach { assertFalse(it.toString(), it.isPrimaryEvidence()) }
    }

    private fun quran() = RetrievedCitation(
        "quran-112-1", "quran", "QS. Al-Ikhlas: 1", "QS. Al-Ikhlas: 1",
        "قُلْ هُوَ اللَّهُ أَحَدٌ", 0.9f, deepLinkTarget = "quran:112:1"
    )
}
