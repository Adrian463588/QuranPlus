package com.quranplus.app

import com.quranplus.app.features.rag.data.VectorRetrieverImpl
import com.quranplus.app.features.rag.domain.VectorIndex
import com.quranplus.app.features.rag.domain.VectorIndexCoverage
import com.quranplus.app.features.rag.domain.VectorMatch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VectorRetrieverCitationTest {

    @Test
    fun GIVEN_semanticHadithMatch_WHEN_mappedToCitation_THEN_exposesExactHadithTarget() {
        val index = FakeVectorIndex(
            VectorMatch(
                sourceId = "hadith-bukhari-1#0",
                sourceType = "hadith",
                collectionId = "bukhari",
                title = "Sahih al-Bukhari",
                reference = "",
                identifier = "1",
                text = "Isi hadist lokal",
                distance = 0.1f
            )
        )

        val citation = runBlocking {
            VectorRetrieverImpl(index).retrieveTopK(
                query = "niat",
                queryEmbedding = floatArrayOf(0.1f),
                k = 5,
                minScore = 0.5f
            ).single()
        }

        assertEquals("hadith:bukhari:1", citation.deepLinkTarget)
        assertTrue(citation.title.contains("No. 1"))
    }

    @Test
    fun GIVEN_semanticQuranChunk_WHEN_mappedToCitation_THEN_exposesExactAyahTarget() {
        val index = FakeVectorIndex(
            VectorMatch(
                sourceId = "quran-2-229#0",
                sourceType = "quran",
                collectionId = "quran",
                title = "QS. Al-Baqarah: 229",
                reference = "QS. Al-Baqarah: 229",
                identifier = "2:229",
                text = "Isi ayat lokal",
                distance = 0.1f
            )
        )

        val citation = runBlocking {
            VectorRetrieverImpl(index).retrieveTopK(
                query = "talak",
                queryEmbedding = floatArrayOf(0.1f),
                k = 5,
                minScore = 0.5f
            ).single()
        }

        assertEquals("quran:2:229", citation.deepLinkTarget)
    }

    private class FakeVectorIndex(
        private val match: VectorMatch
    ) : VectorIndex {
        override suspend fun isReady(): Boolean = true

        override suspend fun coverage(): VectorIndexCoverage =
            VectorIndexCoverage(recordCount = 1, sourceTypes = setOf(match.sourceType))

        override suspend fun replace(records: List<com.quranplus.app.features.rag.domain.VectorRecord>): Int =
            records.size

        override suspend fun search(queryEmbedding: FloatArray, k: Int): List<VectorMatch> =
            listOf(match)
    }
}
