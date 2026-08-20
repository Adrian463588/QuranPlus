package com.quranplus.app.features.rag.domain

data class RetrievedCitation(
    val sourceId: String,
    val sourceType: String, // "quran", "hadith", "tahsin"
    val title: String,
    val reference: String,
    val textSnippet: String,
    val score: Float,
    val collection: String? = null,
    val identifier: String = sourceId,
    val deepLinkTarget: String? = null,
    val surahNumber: Int? = null,
    val ayahNumber: Int? = null
)

interface VectorRetriever {
    suspend fun isIndexReady(): Boolean = false

    suspend fun indexedSourceTypes(): Set<String> = emptySet()

    suspend fun retrieveTopK(
        query: String,
        queryEmbedding: FloatArray,
        k: Int = 5,
        minScore: Float = 0.35f
    ): List<RetrievedCitation>
}
