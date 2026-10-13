package com.quranplus.app

import com.quranplus.app.core.database.QuranDatabase
import com.quranplus.app.core.database.dao.ChatDao
import com.quranplus.app.features.chatbot.data.ChatRepositoryImpl
import com.quranplus.app.features.chatbot.data.InternetResearcher
import com.quranplus.app.features.chatbot.data.LlmRunner
import com.quranplus.app.features.hadith.data.HadithNumbering
import com.quranplus.app.features.chatbot.domain.GenerationEvent
import com.quranplus.app.features.rag.data.EmbeddingService
import com.quranplus.app.features.rag.domain.GenerationStatus
import com.quranplus.app.features.rag.domain.RagPipeline
import com.quranplus.app.features.rag.domain.RagRuntimeCoordinator
import com.quranplus.app.features.rag.domain.RetrievedCitation
import com.quranplus.app.features.rag.domain.VectorRetriever
import com.quranplus.app.features.settings.data.AiPersona
import com.quranplus.app.features.settings.data.PreferencesManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatGroundingFallbackTest {
    private val database = mockk<QuranDatabase>(relaxed = true)
    private val chatDao = mockk<ChatDao>(relaxed = true)
    private val embedder = mockk<EmbeddingService>(relaxed = true)
    private val retriever = mockk<VectorRetriever>(relaxed = true)
    private val runner = mockk<LlmRunner>(relaxed = true)
    private val internet = mockk<InternetResearcher>()
    private val preferences = mockk<PreferencesManager>()
    private val repository = ChatRepositoryImpl(
        chatDao, embedder, retriever, RagPipeline(), runner, database,
        RagRuntimeCoordinator(), internet, preferences
    )

    init {
        every { preferences.onlineResearchEnabled } returns flowOf(true)
        every { chatDao.getMessages(any()) } returns flowOf(emptyList())
        every { runner.isAvailable() } returns false
        coEvery { internet.search(any(), any()) } returns emptyList()
        coEvery { database.quranDao().getAllSurahsOnce() } returns emptyList()
        coEvery { database.quranDao().getAyah(any(), any()) } returns null
        coEvery { database.quranDao().searchAyahsFts(any()) } returns emptyList()
        coEvery { database.hadithDao().searchFts(any()) } returns emptyList()
        coEvery { database.hadithDao().getVerifiedBundleCollectionCounts(any(), any(), any()) } returns emptyList()
    }

    @Test
    fun GIVEN_noLocalEvidence_WHEN_asking_THEN_automaticallyUsesOnlineQuran() = runTest {
        val source = quran().copy(
            providerId = "alquran-cloud",
            deepLinkTarget = "https://alquran.cloud/ayah/112:1/id.indonesian"
        )
        coEvery { internet.search(any(), any()) } returns listOf(source)
        val result = repository.generateRagResponse("session", "?", AiPersona.USTADZ, null)
        assertEquals(listOf(source), result.citations)
        assertTrue((result.tokenStream.toList().single() as GenerationEvent.Replace).text.contains(source.textSnippet))
        assertEquals(GenerationStatus.FALLBACK, result.generationStatus?.value)
        coVerify(exactly = 1) { internet.search(any(), any()) }
    }

    @Test
    fun GIVEN_adequateLocalQuran_WHEN_asking_THEN_skipsOnlineLookup() = runTest {
        semanticResults(listOf(quran()))
        val result = repository.generateRagResponse("session", "ayat", AiPersona.USTADZ, null)
        assertEquals(listOf(quran()), result.citations)
        coVerify(exactly = 0) { internet.search(any(), any()) }
    }

    @Test
    fun GIVEN_indonesianTopic_WHEN_onlineHadithIsEnglish_THEN_acceptsRelevantPrimaryText() = runTest {
        val source = RetrievedCitation(
            "web:sunnah:bukhari:1947", "hadith", "Sahih al-Bukhari 1947", "Bukhari 1947",
            "neither did the fasting persons criticize those who were not fasting", 0.9f,
            collection = "bukhari", identifier = "1947", providerId = "sunnah-public",
            deepLinkTarget = "https://sunnah.com/bukhari:1947"
        )
        coEvery { internet.search(any(), any()) } returns listOf(source)
        val result = repository.generateRagResponse("session", "hadist puasa", AiPersona.USTADZ, null)
        assertEquals(listOf(source), result.citations)
    }

    @Test
    fun GIVEN_editorialOrUserDocument_WHEN_retrieving_THEN_abstainsWithoutGenerating() = runTest {
        semanticResults(listOf(quran().copy(sourceType = "user_document")))
        coEvery { internet.search(any(), any()) } returns listOf(quran().copy(
            sourceType = "internet", providerId = "duckduckgo-web",
            deepLinkTarget = "https://islam.nu.or.id/article"
        ))
        val result = repository.generateRagResponse("session", "ayat", AiPersona.USTADZ, null)
        assertTrue(result.citations.isEmpty())
        assertEquals(GenerationStatus.UNAVAILABLE, result.generationStatus?.value)
        assertTrue((result.tokenStream.toList().single() as GenerationEvent.Replace).text.contains("Belum ditemukan"))
        coVerify(exactly = 0) { runner.generate(any(), any(), any()) }
    }

    @Test
    fun GIVEN_onlineOptOut_WHEN_localEvidenceMissing_THEN_abstainsWithoutNetwork() = runTest {
        every { preferences.onlineResearchEnabled } returns flowOf(false)
        val result = repository.generateRagResponse("session", "?", AiPersona.USTADZ, null)
        assertTrue(result.citations.isEmpty())
        coVerify(exactly = 0) { internet.search(any(), any()) }
    }

    @Test
    fun GIVEN_localRetrievalTimeout_WHEN_asking_THEN_stillTriesOnline() = runTest {
        coEvery { database.quranDao().getAllSurahsOnce() } coAnswers {
            delay(9_000)
            emptyList()
        }
        repository.generateRagResponse("session", "ayat", AiPersona.USTADZ, null)
        coVerify(exactly = 1) { internet.search(any(), any()) }
    }

    @Test
    fun GIVEN_providerFailure_WHEN_asking_THEN_returnsHonestUnavailableAnswer() = runTest {
        coEvery { internet.search(any(), any()) } throws IllegalStateException("provider unavailable")
        val result = repository.generateRagResponse("session", "?", AiPersona.USTADZ, null)
        assertEquals(GenerationStatus.UNAVAILABLE, result.generationStatus?.value)
    }

    @Test(expected = CancellationException::class)
    fun GIVEN_cancelledSearch_WHEN_asking_THEN_propagatesCancellation() = runTest {
        coEvery { internet.search(any(), any()) } throws CancellationException("cancelled")
        repository.generateRagResponse("session", "?", AiPersona.USTADZ, null)
    }

    @Test
    fun GIVEN_invalidGeneratedCitation_WHEN_generating_THEN_replacesWithSourceExcerpt() = runTest {
        semanticResults(listOf(quran()))
        every { runner.isAvailable() } returns true
        every { runner.generate(any(), any(), any()) } returns flowOf("Klaim tanpa rujukan.")
        val result = repository.generateRagResponse("session", "ayat", AiPersona.USTADZ, null)
        val events = result.tokenStream.toList()
        assertTrue(events.all { it is GenerationEvent.Replace })
        assertTrue((events.single() as GenerationEvent.Replace).text.contains(quran().textSnippet))
        assertEquals(GenerationStatus.FALLBACK, result.generationStatus?.value)
    }

    @Test
    fun GIVEN_validGeneratedCitation_WHEN_displayFormattingStripsMarkers_THEN_keepsCompleteStatus() = runTest {
        semanticResults(listOf(quran()))
        every { runner.isAvailable() } returns true
        every { runner.generate(any(), any(), any()) } returns flowOf("Allah Maha Esa. [[cite:C1]]")
        val result = repository.generateRagResponse("session", "ayat", AiPersona.USTADZ, null)
        result.tokenStream.toList()
        assertEquals(GenerationStatus.COMPLETE, result.generationStatus?.value)
    }

    @Test
    fun GIVEN_archiveNumberMismatch_WHEN_askingFor3837_THEN_silentlyRetrievesExactOnlineReference() = runTest {
        val expected = onlineHadith(3837)
        coEvery { internet.search(any(), any()) } returns listOf(onlineHadith(4420), expected)
        val result = repository.generateRagResponse("session", "HR. Abu Dawud no. 3837", AiPersona.USTADZ, null)
        assertEquals(listOf(expected), result.citations)
        coVerify(exactly = 1) { internet.search("HR. Abu Dawud no. 3837", any()) }
    }

    @Test
    fun GIVEN_onlyWrongNumberFromProvider_WHEN_askingFor3837_THEN_abstains() = runTest {
        coEvery { internet.search(any(), any()) } returns listOf(onlineHadith(4420))
        val result = repository.generateRagResponse("session", "Abu Daud 3837", AiPersona.USTADZ, null)
        assertTrue(result.citations.isEmpty())
        assertEquals(GenerationStatus.UNAVAILABLE, result.generationStatus?.value)
    }

    @Test
    fun GIVEN_verifiedCrosswalk_WHEN_askingFor3837_THEN_usesDatesAndButterWithoutNetwork() = runTest {
        coEvery { database.hadithDao().getHadithByNumber("abudawud", 3340) } returns HadithNumberingTest.row(3340)
        val result = numberedRepository().generateRagResponse("session", "Abu Dawud 3837", AiPersona.USTADZ, null)
        val citation = result.citations.single()
        assertEquals("hadith:abudawud:3340", citation.deepLinkTarget)
        assertEquals("https://sunnah.com/abudawud:3837", citation.canonicalUrl)
        assertTrue(citation.textSnippet.contains("mentega dan kurma"))
        coVerify(exactly = 0) { internet.search(any(), any()) }
    }

    @Test
    fun GIVEN_corruptedLocalRow_WHEN_askingFor3837_THEN_triesPrimaryFallback() = runTest {
        val row = HadithNumberingTest.row(3340).copy(textArabic = HadithNumberingTest.row(3837).textArabic)
        coEvery { database.hadithDao().getHadithByNumber("abudawud", 3340) } returns row
        val result = numberedRepository().generateRagResponse("session", "Abu Dawud 3837", AiPersona.USTADZ, null)
        assertTrue(result.citations.isEmpty())
        coVerify(exactly = 1) { internet.search(any(), any()) }
    }

    @Test
    fun GIVEN_explicitArchive3837_WHEN_asking_THEN_usesActualArchiveNarrationWithCanonical4420() = runTest {
        coEvery { database.hadithDao().getHadithByNumber("abudawud", 3837) } returns HadithNumberingTest.row(3837)
        val result = numberedRepository().generateRagResponse("session", "Abu Dawud arsip 3837", AiPersona.USTADZ, null)
        assertTrue(result.citations.single().reference.startsWith("Sunan Abu Dawud no. 4420"))
        coVerify(exactly = 0) { internet.search(any(), any()) }
    }

    @Test
    fun GIVEN_missingArchiveOrInvalidReference_WHEN_asking_THEN_neverSearchesWrongEditionOrGenerates() = runTest {
        listOf("Abu Dawud arsip 999999", "Abu Dawud 99999999999999").forEach { query ->
            val result = numberedRepository().generateRagResponse("session", query, AiPersona.USTADZ, null)
            assertTrue(result.citations.isEmpty())
            assertEquals(GenerationStatus.UNAVAILABLE, result.generationStatus?.value)
        }
        coVerify(exactly = 0) { internet.search(any(), any()) }
    }

    @Test
    fun GIVEN_corruptedArchiveRow_WHEN_asking_THEN_rejectsItDespiteCopiedProvenance() = runTest {
        coEvery { database.hadithDao().getHadithByNumber("abudawud", 3837) } returns
            HadithNumberingTest.row(3837).copy(textArabic = HadithNumberingTest.row(3340).textArabic)
        val result = numberedRepository().generateRagResponse("session", "Abu Dawud arsip 3837", AiPersona.USTADZ, null)
        assertTrue(result.citations.isEmpty())
    }

    @Test
    fun GIVEN_oldVectorNumber_WHEN_retrieving_THEN_rehydratesCurrentReferenceAndText() = runTest {
        semanticResults(listOf(RetrievedCitation(
            "hadith-3003837", "hadith", "Abu Dawud no. 3837", "Abu Dawud no. 3837", "old indexed text", 0.9f,
            collection = "abudawud", identifier = "3837", deepLinkTarget = "hadith:abudawud:3837"
        )))
        coEvery { database.hadithDao().getHadithByNumber("abudawud", 3837) } returns HadithNumberingTest.row(3837)
        val result = numberedRepository().generateRagResponse("session", "hadist merajam", AiPersona.USTADZ, null)
        assertTrue(result.citations.single().reference.startsWith("Sunan Abu Dawud no. 4420"))
        assertTrue(result.citations.single().textSnippet.contains("merajam"))
    }

    @Test
    fun GIVEN_sameOrdinalAcrossEditions_WHEN_localEvidenceIsWeak_THEN_keepsAdequateOnlineFallback() = runTest {
        semanticResults(listOf(RetrievedCitation(
            "hadith-3003837", "hadith", "Abu Dawud no. 3837", "Abu Dawud no. 3837", "old indexed text", 0.65f,
            collection = "abudawud", identifier = "3837", deepLinkTarget = "hadith:abudawud:3837"
        )))
        coEvery { database.hadithDao().getHadithByNumber("abudawud", 3837) } returns HadithNumberingTest.row(3837)
        val online = onlineHadith(3837)
        coEvery { internet.search(any(), any()) } returns listOf(online)
        val result = numberedRepository().generateRagResponse("session", "hadist Abu Dawud", AiPersona.USTADZ, null)
        assertTrue(result.citations.contains(online))
        assertTrue(result.citations.any { it.reference.startsWith("Sunan Abu Dawud no. 4420") })
    }

    @Test
    fun GIVEN_validMarkerWithInventedHadithNumber_WHEN_generating_THEN_replacesWithSourceExcerpt() = runTest {
        semanticResults(listOf(quran()))
        every { runner.isAvailable() } returns true
        every { runner.generate(any(), any(), any()) } returns flowOf("HR. Abu Dawud no. 3837. [[cite:C1]]")
        val result = repository.generateRagResponse("session", "ayat", AiPersona.USTADZ, null)
        val answer = (result.tokenStream.toList().single() as GenerationEvent.Replace).text
        assertTrue(answer.contains(quran().textSnippet))
        assertEquals(GenerationStatus.FALLBACK, result.generationStatus?.value)
    }

    private fun numberedRepository() = ChatRepositoryImpl(
        chatDao, embedder, retriever, RagPipeline(), runner, database, RagRuntimeCoordinator(), internet, preferences,
        HadithNumbering { HadithNumberingTest.assetJson }
    )

    private fun onlineHadith(number: Int) = RetrievedCitation(
        "web:sunnah:abudawud:$number", "hadith", "Abu Dawud $number", "Abu Dawud $number",
        HadithNumberingTest.row(3340).textArabic, 0.9f, collection = "abudawud", identifier = number.toString(),
        deepLinkTarget = "https://sunnah.com/abudawud:$number", providerId = "sunnah-public"
    )

    private fun semanticResults(citations: List<RetrievedCitation>) {
        coEvery { embedder.isReady() } returns true
        coEvery { embedder.embed(any()) } returns floatArrayOf(1f)
        coEvery { retriever.isIndexReady() } returns true
        coEvery { retriever.retrieveTopK(any(), any(), any(), any()) } returns citations
    }

    private fun quran() = RetrievedCitation(
        "quran-112-1", "quran", "QS. Al-Ikhlas: 1", "QS. Al-Ikhlas: 1",
        "قُلْ هُوَ اللَّهُ أَحَدٌ", 0.9f, deepLinkTarget = "quran:112:1"
    )
}
