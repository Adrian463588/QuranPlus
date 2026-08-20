package com.quranplus.app.features.rag.domain

data class VectorRecord(
    val sourceId: String,
    val sourceType: String,
    val collectionId: String,
    val chunkIndex: Int,
    val text: String,
    val embedding: FloatArray,
    val title: String = sourceId,
    val reference: String = sourceId,
    val identifier: String = sourceId,
    val surahNumber: Int? = null,
    val ayahNumber: Int? = null
)

data class VectorMatch(
    val sourceId: String,
    val sourceType: String,
    val collectionId: String,
    val title: String,
    val reference: String,
    val identifier: String,
    val text: String,
    val distance: Float,
    val surahNumber: Int? = null,
    val ayahNumber: Int? = null
)

data class VectorIndexCoverage(
    val recordCount: Int,
    val sourceTypes: Set<String>
) {
    val isPopulated: Boolean
        get() = recordCount > 0
}

sealed interface IndexCorpusResult {
    data class Indexed(val recordCount: Int) : IndexCorpusResult
    data class Blocked(val reason: String) : IndexCorpusResult
}

interface VectorIndex {
    suspend fun isReady(): Boolean
    suspend fun coverage(): VectorIndexCoverage = VectorIndexCoverage(0, emptySet())
    suspend fun replace(records: List<VectorRecord>): Int
    suspend fun search(queryEmbedding: FloatArray, k: Int): List<VectorMatch>
}
