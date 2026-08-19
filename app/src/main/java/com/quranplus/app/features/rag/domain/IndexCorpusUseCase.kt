package com.quranplus.app.features.rag.domain

import com.quranplus.app.features.hadith.domain.HadithSourceManifest

class IndexCorpusUseCase(
    private val vectorIndex: VectorIndex
) {
    suspend operator fun invoke(
        manifest: HadithSourceManifest,
        records: List<VectorRecord>
    ): IndexCorpusResult {
        if (!manifest.bundleAllowed || !manifest.isComplete) {
            return IndexCorpusResult.Blocked("CORPUS_UNAVAILABLE")
        }
        if (manifest.licenseStatus != "verified" || manifest.gradeStatus != "verified") {
            return IndexCorpusResult.Blocked("PROVENANCE_UNAVAILABLE")
        }
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
