package com.quranplus.app

import com.quranplus.app.features.rag.domain.*
import org.junit.Assert.*
import org.junit.Test

class HadithReferenceRequestTest {
    @Test
    fun GIVEN_collectionAliases_WHEN_askingByNumber_THEN_preservesExactReference() {
        listOf("HR. Abu Dawud no. 3837", "Sunan Abi Dawud 3837", "hadist Abu Daud 3837", "abu daeud nomor 3837", "abudawud:3837").forEach {
            assertEquals(HadithReferenceRequest("abudawud", 3837), IslamicQueryAnalyzer.analyze(it).hadithReference)
        }
        assertEquals(HadithReferenceRequest("muslim", 2723, "a"), IslamicQueryAnalyzer.analyze("Muslim 2723a").hadithReference)
        assertNull(IslamicQueryAnalyzer.analyze("Saya makan 3 kurma").hadithReference)
    }

    @Test
    fun GIVEN_exactReference_WHEN_candidateIsDifferentCollectionOrNumber_THEN_rejectsIt() {
        val request = HadithReferenceRequest("abudawud", 3837)
        assertTrue(request.matches(source("abudawud", 3837)))
        assertFalse(request.matches(source("abudawud", 4420)))
        assertFalse(request.matches(source("bukhari", 3837)))
        assertFalse(request.matches(source("abudawud", 3837).copy(canonicalUrl = "https://example.com/abudawud:3837")))
    }

    @Test
    fun GIVEN_variantSuffix_WHEN_validating_THEN_neverSubstitutesAnotherVariant() {
        val request = HadithReferenceRequest("muslim", 2723, "a")
        assertTrue(request.matches(source("muslim", 2723).copy(canonicalUrl = request.url)))
        assertFalse(request.matches(source("muslim", 2723).copy(canonicalUrl = "https://sunnah.com/muslim:2723b")))
    }

    @Test
    fun GIVEN_muslimInBookReference_WHEN_askingWithoutSuffix_THEN_keepsVariantSpecificLabel() {
        val citation = source("muslim", 2723).copy(
            providerId = "local-hadith-bundle", canonicalUrl = null, deepLinkTarget = "hadith:muslim:123",
            reference = "Sahih Muslim no. 2723 (Sunnah.com, Kitab 48, Hadis 101); edisi tafsirq/gading no. 123"
        )
        assertTrue(HadithReferenceRequest("muslim", 2723).matches(citation))
        assertFalse(HadithReferenceRequest("muslim", 2723, "a").matches(citation))
    }

    @Test
    fun GIVEN_archiveEditionOrInvalidNumber_WHEN_parsing_THEN_preservesEditionAndRejectsInvalidReferences() {
        listOf("Abu Dawud arsip 3837", "gading Abu Dawud 3837").forEach {
            assertEquals(HadithReferenceRequest("abudawud", 3837, archive = true), IslamicQueryAnalyzer.analyze(it).hadithReference)
        }
        listOf("Abu Dawud 0", "Abu Dawud -1", "Abu Dawud 99999999999999").forEach {
            assertTrue(HadithReferenceRequest.hasInvalidReference(it))
        }
    }

    private fun source(book: String, number: Int) = RetrievedCitation(
        "web:sunnah:$book:$number", "hadith", book, "$book:$number", "source text", 0.9f,
        collection = book, identifier = number.toString(), deepLinkTarget = "https://sunnah.com/$book:$number",
        canonicalUrl = "https://sunnah.com/$book:$number", providerId = "sunnah-public"
    )
}
