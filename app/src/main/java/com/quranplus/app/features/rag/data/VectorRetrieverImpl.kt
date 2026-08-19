package com.quranplus.app.features.rag.data

import com.quranplus.app.core.database.dao.KnowledgeChunkDao
import com.quranplus.app.core.utils.VecMath
import com.quranplus.app.core.utils.VecMath.toFloatArray
import com.quranplus.app.features.rag.domain.RetrievedCitation
import com.quranplus.app.features.rag.domain.VectorRetriever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VectorRetrieverImpl(
    private val chunkDao: KnowledgeChunkDao
) : VectorRetriever {

    override suspend fun retrieveTopK(
        query: String,
        queryEmbedding: FloatArray,
        k: Int,
        minScore: Float
    ): List<RetrievedCitation> = withContext(Dispatchers.Default) {
        require(k > 0) { "k must be positive" }
        require(query.isNotBlank()) { "query must not be blank" }

        chunkDao.getAllChunks()
            .asSequence()
            .mapNotNull { chunk ->
                val blob = chunk.embedding ?: return@mapNotNull null
                val vector = blob.toFloatArray()
                if (vector.size != queryEmbedding.size || vector.any { !it.isFinite() }) {
                    return@mapNotNull null
                }
                val score = VecMath.cosineSimilarity(queryEmbedding, vector)
                if (!score.isFinite() || score < minScore) return@mapNotNull null
                RetrievedCitation(
                    sourceId = chunk.sourceId,
                    sourceType = chunk.sourceType,
                    title = chunk.title,
                    reference = chunk.sourceId,
                    textSnippet = chunk.textContent,
                    score = score
                )
            }
            .sortedByDescending { it.score }
            .take(k)
            .toList()
    }
}
