package com.quranplus.app

import com.quranplus.app.features.rag.domain.CitationTarget
import com.quranplus.app.features.rag.domain.RetrievedCitation
import com.quranplus.app.features.rag.domain.navigationTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CitationTargetTest {

    @Test
    fun GIVEN_quranCitation_WHEN_targetMetadataIsComplete_THEN_resolvesExactAyah() {
        val citation = citation(
            sourceType = "quran",
            sourceId = "quran-2-227",
            surahNumber = 2,
            ayahNumber = 227
        )

        assertEquals(CitationTarget.Quran(2, 227), citation.navigationTarget())
    }

    @Test
    fun GIVEN_savedQuranCitation_WHEN_onlyDeepLinkExists_THEN_resolvesExactAyah() {
        val citation = citation(
            sourceType = "quran",
            sourceId = "legacy-quran-reference",
            deepLinkTarget = "quran:2:229"
        )

        assertEquals(CitationTarget.Quran(2, 229), citation.navigationTarget())
    }

    @Test
    fun GIVEN_hadithCitation_WHEN_targetMetadataIsComplete_THEN_resolvesCollectionAndNumber() {
        val citation = citation(
            sourceType = "hadith",
            sourceId = "hadith-42",
            collection = "bukhari",
            identifier = "1"
        )

        assertEquals(CitationTarget.Hadith("bukhari", 1), citation.navigationTarget())
    }

    @Test
    fun GIVEN_legacyHadithCitation_WHEN_sourceIdContainsCollectionAndNumber_THEN_resolvesTarget() {
        val citation = citation(
            sourceType = "hadith",
            sourceId = "hadith-muslim-810",
            collection = null,
            identifier = "legacy"
        )

        assertEquals(CitationTarget.Hadith("muslim", 810), citation.navigationTarget())
    }

    @Test
    fun GIVEN_internetCitation_WHEN_httpsTargetExists_THEN_resolvesWebUrl() {
        val citation = citation(
            sourceType = "internet",
            sourceId = "web:id.wikipedia.org:123",
            deepLinkTarget = "https://id.wikipedia.org/wiki/Sejarah_Islam"
        )

        assertEquals(
            CitationTarget.Web("https://id.wikipedia.org/wiki/Sejarah_Islam"),
            citation.navigationTarget()
        )
    }

    @Test
    fun GIVEN_internetCitation_WHEN_httpTargetExists_THEN_rejectsUnsafeUrl() {
        val citation = citation(
            sourceType = "internet",
            sourceId = "web:unsafe:1",
            deepLinkTarget = "http://example.com/unsafe"
        )

        assertNull(citation.navigationTarget())
    }

    @Test
    fun GIVEN_internetCitation_WHEN_httpsHostIsNotAllowlisted_THEN_rejectsUrl() {
        val citation = citation(
            sourceType = "web",
            sourceId = "web:untrusted:1",
            deepLinkTarget = "https://example.com/reference"
        )

        assertNull(citation.navigationTarget())
    }

    @Test
    fun GIVEN_nonSourceCitation_WHEN_resolvingTarget_THEN_returnsNull() {
        val citation = citation(
            sourceType = "user_document",
            sourceId = "note-1"
        )

        assertNull(citation.navigationTarget())
    }

    private fun citation(
        sourceType: String,
        sourceId: String,
        collection: String? = null,
        identifier: String = sourceId,
        deepLinkTarget: String? = null,
        surahNumber: Int? = null,
        ayahNumber: Int? = null
    ) = RetrievedCitation(
        sourceId = sourceId,
        sourceType = sourceType,
        title = "Rujukan",
        reference = "Rujukan",
        textSnippet = "Isi rujukan",
        score = 1f,
        collection = collection,
        identifier = identifier,
        deepLinkTarget = deepLinkTarget,
        surahNumber = surahNumber,
        ayahNumber = ayahNumber
    )
}
