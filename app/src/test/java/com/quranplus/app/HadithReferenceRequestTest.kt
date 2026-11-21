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

    @Test
    fun GIVEN_ahmad4302AndEditionAliases_WHEN_parsing_THEN_routesExactSchemeWithoutConfusion() {
        val risalah = IslamicQueryAnalyzer.analyze("Hadis Musnad Ahmad nomor 4302").hadithReference
        assertEquals(HadithReferenceRequest("ahmad", 4302, archive = false), risalah)
        assertEquals("ar_risalah", risalah?.targetSchemeId())

        listOf(
            "Hadis Musnad Ahmad arsip 4302",
            "Hadis Musnad Ahmad nomor 4302 arsip",
            "Hadis Musnad Ahmad Al-Alamiyah 4302",
            "Hadis Musnad Ahmad no. 4302 (Al-Alamiyah)"
        ).forEach { query ->
            val archiveReq = IslamicQueryAnalyzer.analyze(query).hadithReference
            assertEquals("Query '$query' should target archive_gading", HadithReferenceRequest("ahmad", 4302, archive = true), archiveReq)
            assertEquals("archive_gading", archiveReq?.targetSchemeId())
            assertNull(archiveReq?.url)
        }
    }

    @Test
    fun GIVEN_darimiAliasesAndUrl_WHEN_parsingAndMatching_THEN_noFabricatedSunnahUrlAndLocalMatchWorks() {
        listOf(
            "Hadis Sunan al-Darimi nomor 2841",
            "Sunan ad-Darimi 2841",
            "Musnad ad-Darimi 2841",
            "al-Darimi no. 2841",
            "darimi:2841"
        ).forEach { query ->
            val req = IslamicQueryAnalyzer.analyze(query).hadithReference
            assertEquals("Query '$query' failed", HadithReferenceRequest("darimi", 2841, archive = false), req)
            assertEquals("archive_gading", req?.targetSchemeId())
            assertNull("Must never fabricate Sunnah.com URL for Darimi", req?.url)
        }

        val localDarimi2841 = RetrievedCitation(
            sourceId = "hadith-8102841",
            sourceType = "hadith",
            title = "Sunan al-Darimi no. 2841 (Al-Alamiyah)",
            reference = "Sunan al-Darimi no. 2841 (Al-Alamiyah)",
            textSnippet = "[Ali] radlyallahu 'anhu...",
            score = 1.0f,
            collection = "darimi",
            identifier = "2841",
            deepLinkTarget = "hadith:darimi:2841",
            canonicalUrl = null,
            providerId = "local-hadith-bundle"
        )
        val req = IslamicQueryAnalyzer.analyze("Hadis Sunan al-Darimi nomor 2841").hadithReference!!
        assertTrue(req.matches(localDarimi2841))
        assertFalse(req.matches(localDarimi2841.copy(deepLinkTarget = "hadith:darimi:2805", identifier = "2805")))
        assertFalse(req.matches(localDarimi2841.copy(collection = "bukhari", deepLinkTarget = "hadith:bukhari:2841")))
        assertFalse(req.matches(localDarimi2841.copy(providerId = "sunnah-public")))
    }

    private fun source(book: String, number: Int) = RetrievedCitation(
        "web:sunnah:$book:$number", "hadith", book, "$book:$number", "source text", 0.9f,
        collection = book, identifier = number.toString(), deepLinkTarget = "https://sunnah.com/$book:$number",
        canonicalUrl = "https://sunnah.com/$book:$number", providerId = "sunnah-public"
    )
}
