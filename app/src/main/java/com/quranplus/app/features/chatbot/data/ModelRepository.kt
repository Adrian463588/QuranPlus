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
)

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
        // Check primary internal storage
        val local = File(getModelsDirectory(), filename)
        if (local.exists() && local.length() > 0) return local

        // Check external files dir
        val ext = File(context.getExternalFilesDir(null), "models/$filename")
        if (ext.exists() && ext.length() > 0) return ext

        // Check local tmp / adb push directory
        val tmp = File("/data/local/tmp/llm/$filename")
        if (tmp.exists() && tmp.length() > 0) return tmp

        return local
    }

    fun isModelReady(filename: String): Boolean {
        val file = getModelFile(filename)
        return file.exists() && file.length() > 1024 * 1024 // Greater than 1MB
    }

    fun getActiveModelFile(): File {
        // Return first ready model or default target
        for (cfg in availableModelConfigs) {
            val file = getModelFile(cfg.filename)
            if (file.exists() && file.length() > 1024 * 1024) return file
        }
        return getModelFile("gemma-3-1b-it.litertlm")
    }

    fun isAnyModelReady(): Boolean {
        return availableModelConfigs.any { isModelReady(it.filename) }
    }
}
