package com.quranplus.app

import com.quranplus.app.features.chatbot.data.ModelAssetManifest
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
            sizeDescription = "Belum diverifikasi",
            ramRequirement = "Belum diverifikasi",
            sourceUrl = "https://huggingface.co/litert-community/Gemma3-1B-IT"
        )

        assertFalse(manifest.hasVerifiedManifest)
        assertTrue(manifest.downloadUrl.isBlank())
    }

    @Test
    fun GIVEN_manifestWithoutImmutableRevision_WHEN_checkingDownloadGate_THEN_downloadRemainsBlocked() {
        val manifest = ModelAssetManifest(
            id = "qwen2.5-1.5b-instruct",
            name = "Qwen 2.5 1.5B Instruct",
            filename = "qwen2.5-1.5b-instruct.litertlm",
            sizeDescription = "Belum diverifikasi",
            ramRequirement = "Belum diverifikasi",
            artifactUrl = "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct",
            sourceUrl = "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct",
            licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
            sha256 = "0".repeat(64),
            abi = "arm64-v8a",
            licenseStatus = "verified",
            sizeBytes = 1,
            minimumRamMb = 1024,
            tokenizerId = "tokenizer",
            tokenizerSha256 = "0".repeat(64),
            citation = "Source manifest under review"
        )

        assertFalse(manifest.hasVerifiedManifest)
    }
}
