package com.quranplus.app.features.rag.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Gives interactive model inference priority over background corpus indexing.
 * Interactive inference and background indexing share one exclusive gate, so
 * an index rebuild can never race a live ONNX embedding request.
 */
class RagRuntimeCoordinator {
    private val stateMutex = Mutex()
    private val inferenceMutex = Mutex()
    private val activeInferenceCount = MutableStateFlow(0)

    suspend fun <T> withInference(block: suspend () -> T): T = inferenceMutex.withLock {
        stateMutex.withLock { activeInferenceCount.value += 1 }
        try {
            block()
        } finally {
            stateMutex.withLock {
                activeInferenceCount.value = (activeInferenceCount.value - 1).coerceAtLeast(0)
            }
        }
    }

    /** Indexing owns the same mutex for the complete batch, never per record. */
    suspend fun <T> withExclusiveIndex(block: suspend () -> T): T = inferenceMutex.withLock {
        block()
    }

    suspend fun awaitInferenceIdle() {
        activeInferenceCount.first { it == 0 }
    }
}
