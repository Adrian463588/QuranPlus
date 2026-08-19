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

    val availableModelConfigs = listOf(
        ModelInfo(
            id = "gemma-3-1b-it",
            name = "Gemma 3 1B IT (4-bit)",
            filename = "gemma-3-1b-it.litertlm",
            sizeDescription = "~600 MB",
            ramRequirement = "4 GB+ RAM",
            downloadUrl = "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/gemma-3-1b-it.litertlm",
            isRecommended = true
        ),
        ModelInfo(
            id = "qwen2.5-1.5b",
            name = "Qwen 2.5 1.5B Instruct",
            filename = "qwen2.5-1.5b-instruct.litertlm",
            sizeDescription = "~800 MB",
            ramRequirement = "4 GB+ RAM",
            downloadUrl = "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/main/qwen2.5-1.5b-instruct.litertlm",
            isRecommended = false
        ),
        ModelInfo(
            id = "gemma-4-e2b",
            name = "Gemma 4 E2B Instruct",
            filename = "gemma-4-E2B-it-litertlm.litertlm",
            sizeDescription = "~2.58 GB",
            ramRequirement = "6 GB+ RAM",
            downloadUrl = "https://huggingface.co/litert-community/Gemma-4-E2B-IT/resolve/main/gemma-4-E2B-it-litertlm.litertlm",
            isRecommended = false
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
