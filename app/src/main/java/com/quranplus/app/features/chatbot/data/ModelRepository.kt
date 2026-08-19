package com.quranplus.app.features.chatbot.data

import android.content.Context
import com.quranplus.app.features.rag.data.SafAssetStore
import org.json.JSONObject
import java.io.File

enum class ModelAssetRole {
    CHATBOT,
    EMBEDDING
}

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
    val citation: String = "",
    val role: ModelAssetRole = ModelAssetRole.CHATBOT,
    val embeddingDimension: Int? = null
) {
    val downloadUrl: String
        get() = artifactUrl

    val isRuntimeCompatible: Boolean
        get() = when (role) {
            ModelAssetRole.CHATBOT ->
                format.equals("litertlm", ignoreCase = true) &&
                    runtime.equals("LiteRT-LM", ignoreCase = true)

            ModelAssetRole.EMBEDDING ->
                format.equals("onnx", ignoreCase = true) &&
                    runtime.equals("ONNX Runtime", ignoreCase = true) &&
                    embeddingDimension == 384
        }

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

    val isDownloadable: Boolean
        get() = role == ModelAssetRole.CHATBOT &&
            isRuntimeCompatible &&
            hasVerifiedManifest

    val downloadBlocker: String
        get() = when {
            role == ModelAssetRole.EMBEDDING ->
                "Embedding tidak dapat dipasang dari katalog chatbot. Index saat ini membutuhkan ONNX 384-dimensi dan asset pendamping terverifikasi."

            !isRuntimeCompatible ->
                "Runtime belum didukung aplikasi: $format melalui $runtime. Aplikasi memakai LiteRT-LM."

            artifactUrl.isBlank() ->
                "Artifact belum tersedia pada manifest immutable."

            revision.isBlank() ->
                "Revision immutable belum tersedia."

            licenseStatus != VERIFIED_LICENSE_STATUS ->
                "Status lisensi belum diverifikasi atau belum diterima."

            sha256?.matches(SHA256_PATTERN) != true ->
                "SHA-256 artifact belum diverifikasi."

            tokenizerSha256?.matches(SHA256_PATTERN) != true ->
                "SHA-256 tokenizer belum diverifikasi."

            else -> "Manifest model belum lengkap."
        }

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
            id = "gemma4-e2b-it-litert-lm",
            name = "Gemma 4 E2B IT (LiteRT-LM)",
            filename = "gemma-4-E2B-it.litertlm",
            sizeDescription = "2.41 GiB",
            ramRequirement = "RAM minimum 6 GB (katalog sumber)",
            artifactUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/6b78abd019e61a1ca4cbe3b212d2c9ce8ff38a94/gemma-4-E2B-it.litertlm",
            sourceUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/tree/6b78abd019e61a1ca4cbe3b212d2c9ce8ff38a94",
            licenseUrl = "https://ai.google.dev/gemma/terms",
            sha256 = "181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c",
            isRecommended = true,
            version = "repository-6b78abd0",
            revision = "6b78abd019e61a1ca4cbe3b212d2c9ce8ff38a94",
            abi = "generic-arm64",
            licenseStatus = "requires_acceptance",
            sizeBytes = 2_588_147_712L,
            tokenizerId = "bundled-by-artifact",
            minimumRamMb = 6144,
            citation = "LiteRT Community Gemma 4 E2B IT model card"
        ),
        ModelInfo(
            id = "gemma3-1b-it",
            name = "Gemma 3 1B IT",
            filename = "gemma3-1b-it.litertlm",
            sizeDescription = "556 MiB",
            ramRequirement = "RAM minimum 4 GB (katalog sumber)",
            artifactUrl = "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/6d54daa71cfbffba6b2843c08eeb1a27e7430bf0/gemma3-1b-it-int4.litertlm",
            sourceUrl = "https://huggingface.co/litert-community/Gemma3-1B-IT/tree/6d54daa71cfbffba6b2843c08eeb1a27e7430bf0",
            licenseUrl = "https://ai.google.dev/gemma/terms",
            licenseStatus = "requires_acceptance",
            version = "repository-6d54daa7",
            revision = "6d54daa71cfbffba6b2843c08eeb1a27e7430bf0",
            abi = "generic-arm64",
            sizeBytes = 584_417_280L,
            tokenizerId = "tokenizer.model@6d54daa7",
            minimumRamMb = 4096,
            citation = "LiteRT Community Gemma 3 1B IT model card"
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
        ),
        ModelInfo(
            id = "alif-islamic-v4-base",
            name = "Alif Islamic v4 Base (candidate)",
            filename = "alif-islamic-v4-base.task",
            sizeDescription = "Sekitar 903 MB (sumber model)",
            ramRequirement = "Kebutuhan RAM belum diverifikasi untuk perangkat aplikasi",
            sourceUrl = "https://huggingface.co/ahmedtamseer3/alif-islamic-v4-base",
            licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
            format = "task",
            runtime = "MediaPipe/LiteRT",
            licenseStatus = "unverified",
            citation = "Alif Islamic v4 Base model card; kualitas fiqh dan artifact immutable wajib direview"
        ),
        ModelInfo(
            id = "qwen2.5-1.5b-instruct-duoneural",
            name = "Qwen2.5 1.5B Instruct (GGUF candidate)",
            filename = "qwen2.5-1.5b-instruct.gguf",
            sizeDescription = "Ukuran artifact belum diverifikasi",
            ramRequirement = "Kebutuhan RAM belum diverifikasi",
            sourceUrl = "https://huggingface.co/DuoNeural/Qwen2.5-1.5B-Instruct-LiteRT",
            format = "gguf",
            runtime = "llama.cpp",
            licenseStatus = "unverified",
            citation = "DuoNeural model card; artifact, lisensi turunan, dan runtime wajib direview"
        ),
        ModelInfo(
            id = "gemma3-1b-it-mnn-candidate",
            name = "Gemma 3 1B IT (MNN candidate)",
            filename = "gemma-3-1b-it.mnn",
            sizeDescription = "Ukuran artifact belum diverifikasi",
            ramRequirement = "Kebutuhan RAM belum diverifikasi",
            sourceUrl = "https://huggingface.co/darkmaniac7/Gemma-3-1B-IT-MNN",
            licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
            format = "mnn",
            runtime = "MNN",
            licenseStatus = "unverified",
            citation = "MNN community model card; compatibility dan safety profile wajib direview"
        ),
        ModelInfo(
            id = "qwen3-embedding-0.6b",
            name = "Qwen3 Embedding 0.6B (RAG candidate)",
            filename = "qwen3-embedding-0.6b",
            sizeDescription = "Sekitar 1.21 GB (model card)",
            ramRequirement = "Kebutuhan RAM belum diverifikasi",
            sourceUrl = "https://huggingface.co/Qwen/Qwen3-Embedding-0.6B",
            licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
            format = "safetensors",
            runtime = "Transformers",
            licenseStatus = "unverified",
            citation = "Qwen3 Embedding model card; pipeline ONNX dan dimensi index wajib direview",
            role = ModelAssetRole.EMBEDDING,
            embeddingDimension = 1024
        ),
        ModelInfo(
            id = "all-minilm-l6-v2-onnx",
            name = "all-MiniLM-L6-v2 (ONNX RAG)",
            filename = "all-MiniLM-L6-v2.onnx",
            sizeDescription = "Artifact dan asset tokenizer belum dipin",
            ramRequirement = "Kebutuhan RAM belum diverifikasi",
            sourceUrl = "https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2",
            licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
            format = "onnx",
            runtime = "ONNX Runtime",
            licenseStatus = "unverified",
            citation = "Sentence Transformers all-MiniLM-L6-v2 model card; model, tokenizer, dan checksum wajib dipin",
            role = ModelAssetRole.EMBEDDING,
            embeddingDimension = 384
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
            .filter { it.isDownloadable && !isModelReady(it) }
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
            .put("role", modelInfo.role.name)
            .put("embedding_dimension", modelInfo.embeddingDimension)
            .put("citation", modelInfo.citation)
        safAssetStore.publishText(
            text = manifestJson.toString(),
            relativeDirectory = "manifests",
            filename = "model-" + modelInfo.id + ".json"
        )
    }

    fun isModelReady(modelInfo: ModelInfo): Boolean {
        if (!modelInfo.isDownloadable) return false
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
