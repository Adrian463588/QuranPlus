package com.quranplus.app.features.chatbot.data

import android.content.Context
import java.io.File

data class ModelInfo(
    val id: String,
    val name: String,
    val filename: String,
    val sizeDescription: String,
    val ramRequirement: String,
    val downloadUrl: String,
    val sha256: String? = null,
    val isRecommended: Boolean = false
) {
    val hasVerifiedManifest: Boolean
        get() = sha256?.matches(SHA256_PATTERN) == true

    private companion object {
        val SHA256_PATTERN = Regex("[0-9a-fA-F]{64}")
    }
}

class ModelRepository(private val context: Context) {

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
