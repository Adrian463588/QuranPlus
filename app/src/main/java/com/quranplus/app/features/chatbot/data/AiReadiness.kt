package com.quranplus.app.features.chatbot.data

import com.quranplus.app.core.database.QuranDatabase
import com.quranplus.app.features.hadith.data.HadithBundleManifest
import com.quranplus.app.features.rag.data.EmbeddingService
import com.quranplus.app.features.rag.domain.VectorRetriever

enum class AiBlocker {
    MODEL_UNAVAILABLE,
    EMBEDDER_UNAVAILABLE,
    INDEX_UNAVAILABLE,
    CORPUS_UNAVAILABLE
}

data class AiReadiness(
    val isReady: Boolean,
    val blockers: Set<AiBlocker>,
    val indexedSourceTypes: Set<String> = emptySet(),
    /** True when at least one verified chatbot model can be loaded. */
    val isModelReady: Boolean = false
)

class AiReadinessChecker(
    private val modelRepository: ModelRepository,
    private val embeddingService: EmbeddingService,
    private val vectorRetriever: VectorRetriever,
    private val database: QuranDatabase
) {
    suspend fun check(): AiReadiness = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        modelRepository.restoreVerifiedModelsFromSaf()
        val indexedSourceTypes = vectorRetriever.indexedSourceTypes()
        val isAnyReady = modelRepository.isAnyModelReady()
        val isEmbedderReady = embeddingService.isReady()
        val isIndexReady = vectorRetriever.isIndexReady()
        val hasCorpus = hasRequiredCorpus(indexedSourceTypes)
        val blockers = buildSet {
            if (!isAnyReady) add(AiBlocker.MODEL_UNAVAILABLE)
            if (!isEmbedderReady) add(AiBlocker.EMBEDDER_UNAVAILABLE)
            if (!isIndexReady) add(AiBlocker.INDEX_UNAVAILABLE)
            if (!hasCorpus) add(AiBlocker.CORPUS_UNAVAILABLE)
        }
        AiReadiness(
            isReady = blockers.isEmpty(),
            blockers = blockers,
            indexedSourceTypes = indexedSourceTypes,
            isModelReady = isAnyReady
        )
    }

    private suspend fun hasRequiredCorpus(indexedSourceTypes: Set<String>): Boolean {
        val hadithCounts = database.hadithDao().getVerifiedBundleCollectionCounts(
            sourceRevision = HadithBundleManifest.VERIFIED.revision,
            sourceSha256 = HadithBundleManifest.VERIFIED.archiveSha256,
            licenseStatus = "licensed"
        ).associate { it.collectionId to it.recordCount }
        val hadithAvailable = HadithBundleManifest.VERIFIED.isVerifiedCorpus(hadithCounts)
        val documentSourceTypes = database.knowledgeChunkDao()
            .getAllChunks()
            .map { it.sourceType }
            .toSet()
        return hasRequiredCorpus(indexedSourceTypes, hadithAvailable, documentSourceTypes)
    }
}

internal fun hasRequiredCorpus(
    indexedSourceTypes: Set<String>,
    hadithAvailable: Boolean,
    documentSourceTypes: Set<String>
): Boolean {
    val indexed = indexedSourceTypes.map(String::lowercase).toSet()
    if ("quran" !in indexed) return false
    if (!hadithAvailable || "hadith" !in indexed) return false
    return documentSourceTypes.all { sourceType ->
        sourceType.lowercase() in indexed
    }
}
