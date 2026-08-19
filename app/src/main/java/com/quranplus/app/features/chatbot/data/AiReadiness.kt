package com.quranplus.app.features.chatbot.data

import com.quranplus.app.features.rag.data.EmbeddingService
import com.quranplus.app.features.rag.domain.VectorRetriever

enum class AiBlocker {
    MODEL_UNAVAILABLE,
    EMBEDDER_UNAVAILABLE,
    INDEX_UNAVAILABLE
}

data class AiReadiness(
    val isReady: Boolean,
    val blockers: Set<AiBlocker>
)

class AiReadinessChecker(
    private val modelRepository: ModelRepository,
    private val embeddingService: EmbeddingService,
    private val vectorRetriever: VectorRetriever
) {
    suspend fun check(): AiReadiness {
        modelRepository.restoreVerifiedModelsFromSaf()
        val blockers = buildSet {
            if (!modelRepository.isAnyModelReady()) add(AiBlocker.MODEL_UNAVAILABLE)
            if (!embeddingService.isReady()) add(AiBlocker.EMBEDDER_UNAVAILABLE)
            if (!vectorRetriever.isIndexReady()) add(AiBlocker.INDEX_UNAVAILABLE)
        }
        return AiReadiness(isReady = blockers.isEmpty(), blockers = blockers)
    }
}
