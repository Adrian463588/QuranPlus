package com.quranplus.app.features.rag.data

import com.quranplus.app.features.rag.domain.RetrievedCitation
import com.quranplus.app.features.rag.domain.VectorIndex
import com.quranplus.app.features.rag.domain.VectorRetriever

class VectorRetrieverImpl(
    private val vectorIndex: VectorIndex
) : VectorRetriever {

    override suspend fun isIndexReady(): Boolean = vectorIndex.coverage().isPopulated

    override suspend fun indexedSourceTypes(): Set<String> =
        vectorIndex.coverage().sourceTypes

    override suspend fun retrieveTopK(
        query: String,
        queryEmbedding: FloatArray,
        k: Int,
        minScore: Float
    ): List<RetrievedCitation> {
        require(k > 0) { "k must be positive" }
        require(query.isNotBlank()) { "query must not be blank" }

        if (!vectorIndex.isReady()) {
            throw VectorIndexUnavailable(
                "sqlite-vec index belum tersedia"
            )
        }
        return vectorIndex.search(queryEmbedding, k)
            .mapNotNull { match ->
                val score = (1f - match.distance).coerceIn(-1f, 1f)
                if (score < minScore) return@mapNotNull null
                RetrievedCitation(
                    sourceId = match.sourceId,
                    sourceType = match.sourceType,
                    title = match.title,
                    reference = match.reference,
                    textSnippet = match.text,
                    score = score,
                    collection = match.collectionId.takeIf(String::isNotBlank),
                    identifier = match.identifier,
                    surahNumber = match.surahNumber,
                    ayahNumber = match.ayahNumber
                )
            }
    }
}
