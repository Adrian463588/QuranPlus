package com.quranplus.app.features.chatbot.data

import android.content.Context
import com.quranplus.app.features.rag.data.SafAssetStore
import org.json.JSONObject
import java.io.File

data class ModelAssetManifest(
    val id: String,
    val name: String,
    val filename: String,
    val sizeDescription: String,
    val ramRequirement: String,
    val artifactUrl: String = "",
    val sourceUrl: String = "",
    val licenseUrl: String = "",
    val sha256: String? = null,
    val isRecommended: Boolean = false,
    val version: String = "",
    val revision: String = "",
    val abi: String = "",
    val licenseStatus: String = "unverified",
    val sizeBytes: Long? = null,
    val format: String = "litertlm",
    val runtime: String = "LiteRT-LM",
    val tokenizerId: String = "",
    val tokenizerSha256: String? = null,
    val minimumRamMb: Int? = null,
    val citation: String = ""
) {
    val downloadUrl: String
        get() = artifactUrl

    val hasVerifiedManifest: Boolean
        get() = sha256?.matches(SHA256_PATTERN) == true &&
            revision.isNotBlank() &&
            abi.isNotBlank() &&
            format.isNotBlank() &&
            runtime.isNotBlank() &&
            licenseStatus == VERIFIED_LICENSE_STATUS &&
            licenseUrl.startsWith("https://") &&
            artifactUrl.startsWith("https://") &&
            sourceUrl.startsWith("https://") &&
            sizeBytes != null &&
            sizeBytes > 0 &&
            minimumRamMb != null &&
            minimumRamMb > 0 &&
            tokenizerId.isNotBlank() &&
            tokenizerSha256?.matches(SHA256_PATTERN) == true &&
            citation.isNotBlank()

    private companion object {
        const val VERIFIED_LICENSE_STATUS = "verified"
        val SHA256_PATTERN = Regex("[0-9a-fA-F]{64}")
    }
}

typealias ModelInfo = ModelAssetManifest

class ModelRepository(
    private val context: Context,
    private val safAssetStore: SafAssetStore
) {

    /**
     * Sources remain visible while downloads stay blocked until the complete
     * immutable manifest has been reviewed.
     */
    val availableModelConfigs: List<ModelInfo> = listOf(
        ModelInfo(
            id = "gemma3-1b-it",
            name = "Gemma 3 1B IT",
            filename = "gemma3-1b-it.litertlm",
            sizeDescription = "Artifact belum diverifikasi",
            ramRequirement = "RAM perangkat harus diverifikasi",
            sourceUrl = "https://huggingface.co/litert-community/Gemma3-1B-IT",
            licenseUrl = "https://ai.google.dev/gemma/terms",
            licenseStatus = "requires_acceptance"
        ),
        ModelInfo(
            id = "qwen2.5-1.5b-instruct",
            name = "Qwen 2.5 1.5B Instruct",
            filename = "qwen2.5-1.5b-instruct.litertlm",
            sizeDescription = "Artifact belum diverifikasi",
            ramRequirement = "RAM perangkat harus diverifikasi",
            sourceUrl = "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct",
            licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
            licenseStatus = "unverified"
        )
    )

    fun getModelsDirectory(): File {
        val dir = File(context.filesDir, "models")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getModelFile(filename: String): File {
        require(filename == File(filename).name) { "Model filename must not contain a path" }
        return File(getModelsDirectory(), filename)
    }

    fun isModelReady(filename: String): Boolean {
        val config = availableModelConfigs.firstOrNull { it.filename == filename } ?: return false
        return isModelReady(config)
    }

    fun getActiveModelFile(): File {
        val readyModel = availableModelConfigs.firstOrNull(::isModelReady)
            ?: throw IllegalStateException(
                "Tidak ada model LiteRT-LM terverifikasi. SHA-256 manifest dan file model diperlukan."
            )
        return getModelFile(readyModel.filename)
    }

    fun isAnyModelReady(): Boolean {
        return availableModelConfigs.any(::isModelReady)
    }

    suspend fun restoreVerifiedModelsFromSaf() {
        availableModelConfigs
            .filter { !isModelReady(it) }
            .forEach { model ->
                val destination = getModelFile(model.filename)
                runCatching {
                    safAssetStore.materialize(
                        relativePath = "models/${model.filename}",
                        destination = destination,
                        expectedSha256 = model.sha256.orEmpty()
                    )
                }
            }
    }

    suspend fun persistVerifiedModel(modelInfo: ModelInfo) {
        if (!isModelReady(modelInfo)) return
        safAssetStore.publishFile(
            source = getModelFile(modelInfo.filename),
            relativeDirectory = "models",
            filename = modelInfo.filename
        )
        val manifestJson = JSONObject()
            .put("id", modelInfo.id)
            .put("name", modelInfo.name)
            .put("filename", modelInfo.filename)
            .put("format", modelInfo.format)
            .put("runtime", modelInfo.runtime)
            .put("artifact_url", modelInfo.artifactUrl)
            .put("source_url", modelInfo.sourceUrl)
            .put("license_url", modelInfo.licenseUrl)
            .put("license_status", modelInfo.licenseStatus)
            .put("version", modelInfo.version)
            .put("revision", modelInfo.revision)
            .put("abi", modelInfo.abi)
            .put("size_bytes", modelInfo.sizeBytes)
            .put("sha256", modelInfo.sha256)
            .put("tokenizer_id", modelInfo.tokenizerId)
            .put("tokenizer_sha256", modelInfo.tokenizerSha256)
            .put("minimum_ram_mb", modelInfo.minimumRamMb)
            .put("citation", modelInfo.citation)
        safAssetStore.publishText(
            text = manifestJson.toString(),
            relativeDirectory = "manifests",
            filename = "model-" + modelInfo.id + ".json"
        )
    }

    fun isModelReady(modelInfo: ModelInfo): Boolean {
        if (!modelInfo.hasVerifiedManifest) return false
        val file = getModelFile(modelInfo.filename)
        return file.isFile && file.length() > 0L &&
            calculateSha256(file).equals(modelInfo.sha256, ignoreCase = true)
    }

    private fun calculateSha256(file: File): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
