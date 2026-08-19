package com.quranplus.app.features.rag.domain

data class VectorRecord(
    val sourceId: String,
    val collectionId: String,
    val chunkIndex: Int,
    val text: String,
    val embedding: FloatArray,
    val sourceRevision: String,
    val sourceSha256: String
)

sealed interface IndexCorpusResult {
    data class Indexed(val recordCount: Int) : IndexCorpusResult
    data class Blocked(val reason: String) : IndexCorpusResult
}

interface VectorIndex {
    suspend fun isReady(): Boolean
    suspend fun replace(records: List<VectorRecord>): Int
}
