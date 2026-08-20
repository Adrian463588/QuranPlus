package com.quranplus.app.features.chatbot.data

import com.quranplus.app.core.database.QuranDatabase
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
    val indexedSourceTypes: Set<String> = emptySet()
)

class AiReadinessChecker(
    private val modelRepository: ModelRepository,
    private val embeddingService: EmbeddingService,
    private val vectorRetriever: VectorRetriever,
    private val database: QuranDatabase
) {
    suspend fun check(): AiReadiness {
        modelRepository.restoreVerifiedModelsFromSaf()
        val indexedSourceTypes = vectorRetriever.indexedSourceTypes()
        val blockers = buildSet {
            if (!modelRepository.isAnyModelReady()) add(AiBlocker.MODEL_UNAVAILABLE)
            if (!embeddingService.isReady()) add(AiBlocker.EMBEDDER_UNAVAILABLE)
            if (!vectorRetriever.isIndexReady()) add(AiBlocker.INDEX_UNAVAILABLE)
            if (!hasRequiredCorpus(indexedSourceTypes)) add(AiBlocker.CORPUS_UNAVAILABLE)
        }
        return AiReadiness(
            isReady = blockers.isEmpty(),
            blockers = blockers,
            indexedSourceTypes = indexedSourceTypes
        )
    }

    private suspend fun hasRequiredCorpus(indexedSourceTypes: Set<String>): Boolean {
        val hadithAvailable = database.hadithDao().countHadiths() > 0
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
    if ("quran" !in indexedSourceTypes) return false
    if (hadithAvailable && "hadith" !in indexedSourceTypes) return false
    return documentSourceTypes.all(indexedSourceTypes::contains)
}
