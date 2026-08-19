package com.quranplus.app.features.chatbot.data

import android.content.Context
import com.quranplus.app.features.rag.data.SafAssetStore
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

enum class ModelAssetRole {
    CHATBOT,
    EMBEDDING
}

/** Only fields needed to download, verify, and display an asset. */
data class ModelAssetManifest(
    val id: String,
    val name: String,
    val filename: String,
    val artifactUrl: String = "",
    val sourceUrl: String = "",
    val sha256: String? = null,
    val sizeBytes: Long? = null,
    val format: String = "litertlm",
    val runtime: String = "LiteRT-LM",
    val role: ModelAssetRole = ModelAssetRole.CHATBOT,
    val embeddingDimension: Int? = null,
    val isRecommended: Boolean = false
) {
    val downloadUrl: String
        get() = artifactUrl

    val sizeDescription: String
        get() = sizeBytes?.let { bytes ->
            if (bytes >= GIB) "%.2f GiB".format(bytes.toDouble() / GIB)
            else "%.0f MiB".format(bytes.toDouble() / MIB)
        } ?: "Ukuran belum tersedia"

    val isRuntimeCompatible: Boolean
        get() = when (role) {
            ModelAssetRole.CHATBOT ->
                format.equals("litertlm", true) ||
                    (format.equals("task", true) && runtime.equals("LiteRT-LM", true))

            ModelAssetRole.EMBEDDING ->
                format.equals("onnx", true) &&
                    runtime.equals("ONNX Runtime", true) &&
                    embeddingDimension == EMBEDDING_DIMENSION
        }

    val hasVerifiedManifest: Boolean
        get() = filename == File(filename).name &&
            artifactUrl.startsWith("https://") &&
            sourceUrl.startsWith("https://") &&
            sha256?.matches(SHA256_PATTERN) == true &&
            sizeBytes != null && sizeBytes > 0L &&
            isRuntimeCompatible

    val isDownloadable: Boolean
        get() = hasVerifiedManifest

    val downloadBlocker: String
        get() = when {
            artifactUrl.isBlank() -> "Artifact unduhan belum tersedia."
            !isRuntimeCompatible -> "Format/runtime belum cocok dengan aplikasi."
            sha256?.matches(SHA256_PATTERN) != true -> "SHA-256 artifact belum tersedia."
            sizeBytes == null || sizeBytes <= 0L -> "Ukuran artifact belum tersedia."
            else -> "Manifest asset belum lengkap."
        }

    private companion object {
        const val EMBEDDING_DIMENSION = 384
        const val MIB = 1024L * 1024L
        const val GIB = 1024L * MIB
        val SHA256_PATTERN = Regex("[0-9a-fA-F]{64}")
    }
}

typealias ModelInfo = ModelAssetManifest

class ModelRepository(
    private val context: Context,
    private val safAssetStore: SafAssetStore
) {

    /** URLs and hashes are pinned to immutable files; source-only entries stay non-downloadable. */
    val availableModelConfigs: List<ModelInfo> = listOf(
        ModelInfo(
            id = "qwen2.5-1.5b-instruct",
            name = "Qwen 2.5 1.5B Instruct",
            filename = "qwen2.5-1.5b-instruct.task",
            artifactUrl = "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/fc180c8fdd5092041a35d416dea8a6c0f771f5a2/Qwen2.5-1.5B-Instruct_seq128_q8_ekv4096.task",
            sourceUrl = "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/tree/fc180c8fdd5092041a35d416dea8a6c0f771f5a2",
            sha256 = "98c289e1c43cc592ac535594d5de4bdde449e8dc012ac66909064b6880f8b717",
            sizeBytes = 1_567_364_648L,
            format = "task",
            runtime = "LiteRT-LM",
            isRecommended = true
        ),
        ModelInfo(
            id = "gemma3-1b-it",
            name = "Gemma 3 1B IT",
            filename = "gemma3-1b-it.litertlm",
            sourceUrl = "https://huggingface.co/litert-community/Gemma3-1B-IT",
            format = "litertlm",
            runtime = "LiteRT-LM"
        ),
        ModelInfo(
            id = "alif-islamic-v4-base",
            name = "Alif Islamic v4 Base",
            filename = "alif-islamic-v4-base.task",
            sourceUrl = "https://huggingface.co/ahmedtamseer3/alif-islamic-v4-base",
            format = "task",
            runtime = "LiteRT-LM"
        ),
        ModelInfo(
            id = "qwen2.5-1.5b-instruct-duoneural",
            name = "Qwen 2.5 1.5B Instruct (GGUF)",
            filename = "qwen2.5-1.5b-instruct.gguf",
            sourceUrl = "https://huggingface.co/DuoNeural/Qwen2.5-1.5B-Instruct-LiteRT",
            format = "gguf",
            runtime = "llama.cpp"
        ),
        ModelInfo(
            id = "all-minilm-l6-v2-onnx",
            name = "all-MiniLM-L6-v2 (ONNX RAG)",
            filename = "all-MiniLM-L6-v2.onnx",
            artifactUrl = "https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/1110a243fdf4706b3f48f1d95db1a4f5529b4d41/onnx/model_qint8_arm64.onnx",
            sourceUrl = "https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/tree/1110a243fdf4706b3f48f1d95db1a4f5529b4d41",
            sha256 = "4278337fd0ff3c68bfb6291042cad8ab363e1d9fbc43dcb499fe91c871902474",
            sizeBytes = 23_026_053L,
            format = "onnx",
            runtime = "ONNX Runtime",
            role = ModelAssetRole.EMBEDDING,
            embeddingDimension = 384
        ),
        ModelInfo(
            id = "qwen3-embedding-0.6b",
            name = "Qwen3 Embedding 0.6B",
            filename = "qwen3-embedding-0.6b",
            sourceUrl = "https://huggingface.co/Qwen/Qwen3-Embedding-0.6B",
            format = "safetensors",
            runtime = "Transformers",
            role = ModelAssetRole.EMBEDDING,
            embeddingDimension = 1024
        )
    )

    fun getModelsDirectory(): File {
        val directory = File(context.filesDir, "models")
        if (!directory.exists()) directory.mkdirs()
        return directory
    }

    fun getModelFile(filename: String): File {
        require(filename == File(filename).name) { "Model filename must not contain a path" }
        return File(getModelsDirectory(), filename)
    }

    fun isModelReady(filename: String): Boolean =
        availableModelConfigs.firstOrNull { it.filename == filename }?.let(::isModelReady) == true

    fun getActiveModelFile(): File = availableModelConfigs
        .filter { it.role == ModelAssetRole.CHATBOT }
        .firstOrNull(::isModelReady)
        ?.let { getModelFile(it.filename) }
        ?: error("Model LiteRT-LM belum tersedia")

    fun isAnyModelReady(): Boolean = availableModelConfigs
        .filter { it.role == ModelAssetRole.CHATBOT }
        .any(::isModelReady)

    suspend fun restoreVerifiedModelsFromSaf() {
        availableModelConfigs
            .filter { it.isDownloadable && !isModelReady(it) }
            .forEach { model ->
                runCatching {
                    safAssetStore.materialize(
                        relativePath = "models/${model.filename}",
                        destination = getModelFile(model.filename),
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
            text = JSONObject()
                .put("id", modelInfo.id)
                .put("filename", modelInfo.filename)
                .put("size_bytes", modelInfo.sizeBytes)
                .put("sha256", modelInfo.sha256)
                .put("format", modelInfo.format)
                .put("runtime", modelInfo.runtime)
                .put("role", modelInfo.role.name)
                .toString(),
            relativeDirectory = "manifests",
            filename = "model-${modelInfo.id}.json"
        )
    }

    fun isModelReady(modelInfo: ModelInfo): Boolean {
        if (!modelInfo.isDownloadable) return false
        val file = getModelFile(modelInfo.filename)
        return file.isFile && file.length() == modelInfo.sizeBytes &&
            calculateSha256(file).equals(modelInfo.sha256, ignoreCase = true)
    }

    private fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
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
