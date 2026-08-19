package com.quranplus.app

import com.quranplus.app.features.chatbot.data.ModelAssetManifest
import com.quranplus.app.features.chatbot.data.ModelAssetRole
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelAssetManifestTest {

    @Test
    fun GIVEN_sourceOnlyManifest_WHEN_checkingDownloadGate_THEN_downloadRemainsBlocked() {
        val manifest = ModelAssetManifest(
            id = "gemma3-1b-it",
            name = "Gemma 3 1B IT",
            filename = "gemma3-1b-it.litertlm",
            sourceUrl = "https://huggingface.co/litert-community/Gemma3-1B-IT"
        )

        assertFalse(manifest.hasVerifiedManifest)
        assertFalse(manifest.isDownloadable)
    }

    @Test
    fun GIVEN_pinnedQwenArtifact_WHEN_checkingDownloadGate_THEN_downloadIsAllowed() {
        val manifest = ModelAssetManifest(
            id = "qwen",
            name = "Qwen",
            filename = "qwen.task",
            artifactUrl = "https://example.com/qwen.task",
            sourceUrl = "https://example.com/source",
            sha256 = "a".repeat(64),
            sizeBytes = 10,
            format = "task",
            runtime = "LiteRT-LM"
        )

        assertTrue(manifest.hasVerifiedManifest)
        assertTrue(manifest.isDownloadable)
    }

    @Test
    fun GIVEN_onnxEmbedding_WHEN_checkingRoleGate_THEN_embeddingCanBeDownloaded() {
        val manifest = ModelAssetManifest(
            id = "minilm",
            name = "all-MiniLM-L6-v2",
            filename = "model.onnx",
            artifactUrl = "https://example.com/model.onnx",
            sourceUrl = "https://example.com/source",
            sha256 = "b".repeat(64),
            sizeBytes = 10,
            format = "onnx",
            runtime = "ONNX Runtime",
            role = ModelAssetRole.EMBEDDING,
            embeddingDimension = 384
        )

        assertTrue(manifest.isRuntimeCompatible)
        assertTrue(manifest.isDownloadable)
    }

    @Test
    fun GIVEN_ggufCandidate_WHEN_checkingRuntimeGate_THEN_downloadRemainsBlocked() {
        val manifest = ModelAssetManifest(
            id = "qwen-gguf",
            name = "Qwen GGUF",
            filename = "qwen.gguf",
            artifactUrl = "https://example.com/qwen.gguf",
            sourceUrl = "https://example.com/source",
            sha256 = "c".repeat(64),
            sizeBytes = 10,
            format = "gguf",
            runtime = "llama.cpp"
        )

        assertFalse(manifest.isRuntimeCompatible)
        assertFalse(manifest.isDownloadable)
    }
}
