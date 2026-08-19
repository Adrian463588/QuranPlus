package com.quranplus.app.features.chatbot.data

import android.content.Context
import com.quranplus.app.features.rag.data.SafAssetStore
import java.io.File

data class ModelInfo(
    val id: String,
    val name: String,
    val filename: String,
    val sizeDescription: String,
    val ramRequirement: String,
    val downloadUrl: String,
    val sha256: String? = null,
    val isRecommended: Boolean = false,
    val version: String = "",
    val abi: String = "",
    val licenseStatus: String = "unverified",
    val sizeBytes: Long? = null
) {
    val hasVerifiedManifest: Boolean
        get() = sha256?.matches(SHA256_PATTERN) == true &&
            version.isNotBlank() &&
            abi.isNotBlank() &&
            licenseStatus == VERIFIED_LICENSE_STATUS &&
            downloadUrl.startsWith("https://")

    private companion object {
        const val VERIFIED_LICENSE_STATUS = "verified"
        val SHA256_PATTERN = Regex("[0-9a-fA-F]{64}")
    }
}

class ModelRepository(
    private val context: Context,
    private val safAssetStore: SafAssetStore
) {

    /**
     * Model catalog is intentionally empty until a reviewed, pinned manifest is
     * supplied. A URL without an exact SHA-256 is not a downloadable production
     * model and must not appear as one in the UI.
     */
    val availableModelConfigs: List<ModelInfo> = emptyList()

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
        safAssetStore.publishText(
            text = "{\"id\":\"${modelInfo.id}\",\"filename\":\"${modelInfo.filename}\",\"version\":\"${modelInfo.version}\",\"abi\":\"${modelInfo.abi}\",\"size_bytes\":${modelInfo.sizeBytes ?: 0},\"sha256\":\"${modelInfo.sha256}\",\"license_status\":\"${modelInfo.licenseStatus}\"}",
            relativeDirectory = "manifests",
            filename = "model-${modelInfo.id}.json"
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
