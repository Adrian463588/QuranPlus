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
            artifactUrl = "https://example.com/resolve/0123456789abcdef0123456789abcdef01234567/qwen.task",
            sourceUrl = "https://example.com/tree/0123456789abcdef0123456789abcdef01234567",
            sha256 = "a".repeat(64),
            sizeBytes = 10,
            format = "task",
            runtime = "LiteRT-LM",
            licenseId = "Apache-2.0",
            licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0"
        )

        assertTrue(manifest.hasVerifiedManifest)
        assertTrue(manifest.isDownloadable)
    }

    @Test
    fun GIVEN_pinnedGemma4InstructionTunedArtifact_WHEN_checkingDownloadGate_THEN_downloadIsAllowed() {
        val manifest = ModelAssetManifest(
            id = "gemma4-e2b-it",
            name = "Gemma 4 E2B IT (instruction-tuned)",
            filename = "gemma-4-E2B-it.litertlm",
            artifactUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/6e5c4f1e395deb959c494953478fa5cec4b8008f/gemma-4-E2B-it.litertlm",
            sourceUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/tree/6e5c4f1e395deb959c494953478fa5cec4b8008f",
            sha256 = "181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c",
            sizeBytes = 2_588_147_712L,
            format = "litertlm",
            runtime = "LiteRT-LM",
            licenseId = "Gemma Terms",
            licenseUrl = "https://ai.google.dev/gemma/terms"
        )

        assertTrue(manifest.isRuntimeCompatible)
        assertTrue(manifest.hasVerifiedManifest)
        assertTrue(manifest.isDownloadable)
    }

    @Test
    fun GIVEN_onnxEmbedding_WHEN_checkingRoleGate_THEN_embeddingCanBeDownloaded() {
        val manifest = ModelAssetManifest(
            id = "minilm",
            name = "all-MiniLM-L6-v2",
            filename = "model.onnx",
            artifactUrl = "https://example.com/resolve/0123456789abcdef0123456789abcdef01234567/model.onnx",
            sourceUrl = "https://example.com/tree/0123456789abcdef0123456789abcdef01234567",
            sha256 = "b".repeat(64),
            sizeBytes = 10,
            format = "onnx",
            runtime = "ONNX Runtime",
            role = ModelAssetRole.EMBEDDING,
            embeddingDimension = 384,
            tokenizerAsset = "embedding/vocab.txt",
            tokenizerType = "wordpiece",
            tokenizerSha256 = "f".repeat(64),
            licenseId = "Apache-2.0",
            licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0"
        )

        assertTrue(manifest.isRuntimeCompatible)
        assertTrue(manifest.isDownloadable)
    }

    @Test
    fun GIVEN_embeddingWithoutMatchingTokenizer_WHEN_checkingRuntimeGate_THEN_downloadRemainsBlocked() {
        val manifest = ModelAssetManifest(
            id = "unsupported-embedding",
            name = "Embedding tanpa tokenizer",
            filename = "embedding.onnx",
            artifactUrl = "https://example.com/resolve/0123456789abcdef0123456789abcdef01234567/embedding.onnx",
            sourceUrl = "https://example.com/tree/0123456789abcdef0123456789abcdef01234567",
            sha256 = "e".repeat(64),
            sizeBytes = 10,
            format = "onnx",
            runtime = "ONNX Runtime",
            role = ModelAssetRole.EMBEDDING,
            embeddingDimension = 384,
            licenseId = "Apache-2.0",
            licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0"
        )

        assertFalse(manifest.isRuntimeCompatible)
        assertFalse(manifest.isDownloadable)
    }

    @Test
    fun GIVEN_sentencePieceEmbeddingWithoutRuntime_WHEN_checkingRuntimeGate_THEN_downloadRemainsBlocked() {
        val manifest = ModelAssetManifest(
            id = "multilingual-minilm",
            name = "Multilingual MiniLM",
            filename = "multilingual.onnx",
            artifactUrl = "https://example.com/resolve/0123456789abcdef0123456789abcdef01234567/model.onnx",
            sourceUrl = "https://example.com/tree/0123456789abcdef0123456789abcdef01234567",
            sha256 = "f".repeat(64),
            sizeBytes = 10,
            format = "onnx",
            runtime = "ONNX Runtime",
            role = ModelAssetRole.EMBEDDING,
            embeddingDimension = 384,
            tokenizerAsset = "embedding/sentencepiece.bpe.model",
            tokenizerType = "sentencepiece",
            tokenizerSha256 = "1".repeat(64),
            licenseId = "Apache-2.0",
            licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0"
        )

        assertFalse(manifest.isRuntimeCompatible)
        assertFalse(manifest.isDownloadable)
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

    @Test
    fun GIVEN_mediapipeTask_WHEN_litertLmIsTheOnlyRuntime_THEN_downloadRemainsBlocked() {
        val manifest = ModelAssetManifest(
            id = "alif",
            name = "Alif Islamic v4 Base",
            filename = "alif.task",
            artifactUrl = "https://example.com/resolve/0123456789abcdef0123456789abcdef01234567/alif.task",
            sourceUrl = "https://example.com/tree/0123456789abcdef0123456789abcdef01234567",
            sha256 = "d".repeat(64),
            sizeBytes = 10,
            format = "task",
            runtime = "MediaPipe LLM",
            licenseId = "Apache-2.0",
            licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0"
        )

        assertFalse(manifest.isRuntimeCompatible)
        assertFalse(manifest.isDownloadable)
    }

    @Test
    fun GIVEN_pinnedMiniLmV2Artifact_WHEN_checkingDownloadGate_THEN_downloadIsAllowedWithTrueLfsSha256() {
        val manifest = ModelAssetManifest(
            id = "all-minilm-l6-v2-onnx",
            name = "all-MiniLM-L6-v2 (ONNX RAG)",
            filename = "model_qint8_arm64.onnx",
            artifactUrl = "https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/1110a243fdf4706b3f48f1d95db1a4f5529b4d41/onnx/model_qint8_arm64.onnx",
            sourceUrl = "https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/tree/1110a243fdf4706b3f48f1d95db1a4f5529b4d41",
            sha256 = "4278337fd0ff3c68bfb6291042cad8ab363e1d9fbc43dcb499fe91c871902474",
            sizeBytes = 23_026_053L,
            format = "onnx",
            runtime = "ONNX Runtime",
            role = ModelAssetRole.EMBEDDING,
            embeddingDimension = 384,
            tokenizerAsset = "embedding/vocab.txt",
            tokenizerType = "wordpiece",
            tokenizerSha256 = "07eced375cec144d27c900241f3e339478dec958f92fddbc551f295c992038a3",
            licenseId = "Apache-2.0",
            licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
            isRecommended = true
        )

        assertTrue(manifest.isRuntimeCompatible)
        assertTrue(manifest.hasVerifiedManifest)
        assertTrue(manifest.isDownloadable)
    }

    @Test
    fun GIVEN_modelDownloadNotificationManager_WHEN_checkingConstants_THEN_channelAndIdAreConsistent() {
        org.junit.Assert.assertEquals("quran_model_download_channel", com.quranplus.app.features.chatbot.data.ModelDownloadNotificationManager.CHANNEL_ID)
        org.junit.Assert.assertEquals(2001, com.quranplus.app.features.chatbot.data.ModelDownloadNotificationManager.NOTIFICATION_ID)
    }
}
