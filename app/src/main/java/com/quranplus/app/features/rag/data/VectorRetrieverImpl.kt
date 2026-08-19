package com.quranplus.app.features.rag.data

import com.quranplus.app.features.rag.domain.RetrievedCitation
import com.quranplus.app.features.rag.domain.VectorIndex
import com.quranplus.app.features.rag.domain.VectorRetriever

class VectorIndexUnavailable(message: String) : IllegalStateException(message)

class VectorRetrieverImpl(
    private val vectorIndex: VectorIndex
) : VectorRetriever {

    override suspend fun isIndexReady(): Boolean = vectorIndex.isReady()

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
                "sqlite-vec index unavailable; Room chunk scan is intentionally disabled"
            )
        }
        throw VectorIndexUnavailable(
            "sqlite-vec retrieval contract is not available for this asset"
        )
    }
}
