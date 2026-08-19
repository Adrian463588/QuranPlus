package com.quranplus.app.features.rag.domain

class IndexCorpusUseCase(
    private val vectorIndex: VectorIndex
) {
    suspend operator fun invoke(
        records: List<VectorRecord>
    ): IndexCorpusResult {
        if (records.isEmpty() || records.any { it.embedding.size != EMBEDDING_DIMENSION }) {
            return IndexCorpusResult.Blocked("EMBEDDER_UNAVAILABLE")
        }
        if (!vectorIndex.isReady()) {
            return IndexCorpusResult.Blocked("INDEX_UNAVAILABLE")
        }
        return IndexCorpusResult.Indexed(vectorIndex.replace(records))
    }

    private companion object {
        const val EMBEDDING_DIMENSION = 384
    }
}
