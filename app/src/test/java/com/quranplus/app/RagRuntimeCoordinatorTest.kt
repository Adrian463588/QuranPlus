package com.quranplus.app

import com.quranplus.app.features.rag.domain.RagRuntimeCoordinator
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RagRuntimeCoordinatorTest {

    @Test
    fun GIVEN_inferenceIsActive_WHEN_indexerWaits_THEN_indexerResumesAfterInference() = runTest {
        val coordinator = RagRuntimeCoordinator()
        val inferenceStarted = CompletableDeferred<Unit>()
        val releaseInference = CompletableDeferred<Unit>()

        val inference = async {
            coordinator.withInference {
                inferenceStarted.complete(Unit)
                releaseInference.await()
            }
        }
        inferenceStarted.await()

        val indexStep = async {
            coordinator.awaitInferenceIdle()
            true
        }

        assertFalse(indexStep.isCompleted)
        releaseInference.complete(Unit)

        assertTrue(indexStep.await())
        inference.await()
    }

    @Test
    fun GIVEN_indexingOwnsInferenceGate_WHEN_chatStarts_THEN_chatWaitsUntilIndexCompletes() = runTest {
        val coordinator = RagRuntimeCoordinator()
        val indexStarted = CompletableDeferred<Unit>()
        val releaseIndex = CompletableDeferred<Unit>()

        val index = async {
            coordinator.withExclusiveIndex {
                indexStarted.complete(Unit)
                releaseIndex.await()
            }
        }
        indexStarted.await()

        val inference = async {
            coordinator.withInference { true }
        }
        assertFalse(inference.isCompleted)
        releaseIndex.complete(Unit)

        assertTrue(inference.await())
        index.await()
    }
}
