package com.quranplus.app.features.rag.data

import com.quranplus.app.core.database.dao.KnowledgeChunkDao
import com.quranplus.app.features.rag.domain.RetrievedCitation
import com.quranplus.app.features.rag.domain.VectorRetriever

class VectorIndexUnavailable(message: String) : IllegalStateException(message)

class VectorRetrieverImpl(
    private val chunkDao: KnowledgeChunkDao
) : VectorRetriever {

    override suspend fun retrieveTopK(
        query: String,
        queryEmbedding: FloatArray,
        k: Int,
        minScore: Float
    ): List<RetrievedCitation> {
        require(k > 0) { "k must be positive" }
        require(query.isNotBlank()) { "query must not be blank" }

        if (chunkDao.getChunksCount() == 0) return emptyList()
        throw VectorIndexUnavailable(
            "sqlite-vec index is unavailable; Room chunk scan is intentionally disabled"
        )
    }
}
