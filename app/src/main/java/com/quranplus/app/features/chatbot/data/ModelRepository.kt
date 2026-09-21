package com.quranplus.app.features.chatbot.data

import android.content.Context
import com.quranplus.app.features.rag.data.SafAssetStore
import org.json.JSONObject
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    /** Tokenizer contract used by the ONNX embedder; null means unsupported by this build. */
    val tokenizerAsset: String? = null,
    val tokenizerType: String? = null,
    val tokenizerSha256: String? = null,
    val licenseId: String = "",
    val licenseUrl: String = "",
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
                    embeddingDimension == EMBEDDING_DIMENSION &&
                    tokenizerSha256?.matches(SHA256_PATTERN) == true &&
                    ((tokenizerType.equals("wordpiece", true) && tokenizerAsset == WORDPIECE_VOCABULARY_ASSET) ||
                     (tokenizerType.equals("sentencepiece", true) && tokenizerAsset == SENTENCEPIECE_VOCABULARY_ASSET))
        }

    val hasVerifiedManifest: Boolean
        get() = filename == File(filename).name &&
            isPinnedHttpsArtifact(artifactUrl) &&
            isPinnedHttpsSource(sourceUrl) &&
            sha256?.matches(SHA256_PATTERN) == true &&
            sizeBytes != null && sizeBytes > 0L &&
            licenseId.isNotBlank() &&
            licenseUrl.startsWith("https://") &&
            isRuntimeCompatible

    val isDownloadable: Boolean
        get() = hasVerifiedManifest

    val downloadBlocker: String
        get() = when {
            artifactUrl.isBlank() -> "Artifact unduhan belum tersedia."
            role == ModelAssetRole.EMBEDDING && tokenizerAsset.isNullOrBlank() ->
                "Tokenizer yang cocok belum tersedia di aplikasi."
            role == ModelAssetRole.EMBEDDING && tokenizerType.isNullOrBlank() ->
                "Jenis tokenizer artifact belum dikontrak."
            role == ModelAssetRole.EMBEDDING &&
                !tokenizerType.equals("wordpiece", true) &&
                !tokenizerType.equals("sentencepiece", true) ->
                "Tokenizer artifact belum didukung oleh runtime aplikasi."
            role == ModelAssetRole.EMBEDDING && tokenizerSha256?.matches(SHA256_PATTERN) != true ->
                "SHA-256 tokenizer belum tersedia."
            !isRuntimeCompatible -> "Format/runtime belum cocok dengan aplikasi."
            !isPinnedHttpsArtifact(artifactUrl) || !isPinnedHttpsSource(sourceUrl) ->
                "Artifact harus memakai revisi sumber yang dipin."
            sha256?.matches(SHA256_PATTERN) != true -> "SHA-256 artifact belum tersedia."
            sizeBytes == null || sizeBytes <= 0L -> "Ukuran artifact belum tersedia."
            licenseId.isBlank() || !licenseUrl.startsWith("https://") ->
                "Lisensi artifact belum tersedia."
            else -> "Manifest asset belum lengkap."
        }

    private fun isPinnedHttpsArtifact(url: String): Boolean =
        url.startsWith("https://") && PINNED_ARTIFACT_PATTERN.containsMatchIn(url)

    private fun isPinnedHttpsSource(url: String): Boolean =
        url.startsWith("https://") && PINNED_SOURCE_PATTERN.containsMatchIn(url)

    private companion object {
        const val EMBEDDING_DIMENSION = 384
        const val WORDPIECE_VOCABULARY_ASSET = "embedding/vocab.txt"
        const val SENTENCEPIECE_VOCABULARY_ASSET = "embedding/sentencepiece.bpe.model"
        const val MIB = 1024L * 1024L
        const val GIB = 1024L * MIB
        val SHA256_PATTERN = Regex("[0-9a-fA-F]{64}")
        val PINNED_ARTIFACT_PATTERN = Regex("/resolve/[0-9a-fA-F]{40}/")
        val PINNED_SOURCE_PATTERN = Regex("/tree/[0-9a-fA-F]{40}(?:$|[/?#])")
    }
}

typealias ModelInfo = ModelAssetManifest

class ModelRepository(
    private val context: Context,
    private val safAssetStore: SafAssetStore
) {

    /** Only artifacts with a real pinned URL, size, SHA-256, and license are downloadable. */
    val availableModelConfigs: List<ModelInfo> = listOf(
        ModelInfo(
            id = "qwen2.5-1.5b-instruct",
            name = "Qwen 2.5 1.5B Instruct",
            filename = "Qwen2.5-1.5B-Instruct_seq128_q8_ekv4096.task",
            artifactUrl = "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/fc180c8fdd5092041a35d416dea8a6c0f771f5a2/Qwen2.5-1.5B-Instruct_seq128_q8_ekv4096.task",
            sourceUrl = "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/tree/fc180c8fdd5092041a35d416dea8a6c0f771f5a2",
            sha256 = "98c289e1c43cc592ac535594d5de4bdde449e8dc012ac66909064b6880f8b717",
            sizeBytes = 1_567_364_648L,
            format = "task",
            runtime = "LiteRT-LM",
            licenseId = "Apache-2.0",
            licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
            isRecommended = true
        ),
        ModelInfo(
            id = "alif-islamic-v4-base",
            name = "Alif Islamic v4 Base",
            filename = "alif-islamic-v4-base.task",
            artifactUrl = "https://huggingface.co/ahmedtamseer3/alif-islamic-v4-base/resolve/f7847ebcc1568007ab585bcaf1bffd062bca534c/alif-islamic-v4-base.task",
            sourceUrl = "https://huggingface.co/ahmedtamseer3/alif-islamic-v4-base/tree/f7847ebcc1568007ab585bcaf1bffd062bca534c",
            sha256 = "79deeca9f2120c08454ccb09f0399b42d4b3146e8d3fdc0bde4cfa2787f2bbaa",
            sizeBytes = 946_786_704L,
            format = "task",
            runtime = "LiteRT-LM",
            licenseId = "Apache-2.0",
            licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
            isRecommended = false
        ),
        ModelInfo(
            id = "gemma4-e2b-it",
            name = "Gemma 4 E2B IT",
            filename = "gemma-4-E2B-it.litertlm",
            artifactUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/6e5c4f1e395deb959c494953478fa5cec4b8008f/gemma-4-E2B-it.litertlm",
            sourceUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/tree/6e5c4f1e395deb959c494953478fa5cec4b8008f",
            sha256 = "181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c",
            sizeBytes = 2_588_147_712L,
            format = "litertlm",
            runtime = "LiteRT-LM",
            licenseId = "Gemma Terms",
            licenseUrl = "https://ai.google.dev/gemma/terms"
        ),
        ModelInfo(
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
        ),
        ModelInfo(
            id = "bge-small-en-v1.5-onnx",
            name = "BAAI BGE Small EN v1.5 (ONNX)",
            filename = "bge_small_en_v1.5.onnx",
            artifactUrl = "https://huggingface.co/BAAI/bge-small-en-v1.5/resolve/5c38ec7c405ec4b44b94cc5a9bb96e735b38267a/onnx/model.onnx",
            sourceUrl = "https://huggingface.co/BAAI/bge-small-en-v1.5/tree/5c38ec7c405ec4b44b94cc5a9bb96e735b38267a",
            sha256 = "828e1496d7fabb79cfa4dcd84fa38625c0d3d21da474a00f08db0f559940cf35",
            sizeBytes = 133_093_490L,
            format = "onnx",
            runtime = "ONNX Runtime",
            role = ModelAssetRole.EMBEDDING,
            embeddingDimension = 384,
            tokenizerAsset = "embedding/vocab.txt",
            tokenizerType = "wordpiece",
            tokenizerSha256 = "07eced375cec144d27c900241f3e339478dec958f92fddbc551f295c992038a3",
            licenseId = "MIT",
            licenseUrl = "https://opensource.org/licenses/MIT"
        ),
        ModelInfo(
            id = "paraphrase-multilingual-minilm-l12-v2-onnx",
            name = "Multilingual MiniLM L12 v2 (ONNX)",
            filename = "multilingual_minilm_l12_v2_qint8.onnx",
            artifactUrl = "https://huggingface.co/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2/resolve/e8f8c211226b894fcb81acc59f3b34ba3efd5f42/onnx/model_qint8_arm64.onnx",
            sourceUrl = "https://huggingface.co/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2/tree/e8f8c211226b894fcb81acc59f3b34ba3efd5f42",
            sha256 = "783fea82d71a58179b830a4dbd2d58447e640609e98eedf9ffa12622d375a672",
            sizeBytes = 118_412_398L,
            format = "onnx",
            runtime = "ONNX Runtime",
            role = ModelAssetRole.EMBEDDING,
            embeddingDimension = 384,
            tokenizerAsset = "embedding/sentencepiece.bpe.model",
            tokenizerType = "sentencepiece",
            tokenizerSha256 = "cfc8146abe2a0488e9e2a0c56de7952f7c11ab059eca145a0a727afce0db2865",
            licenseId = "Apache-2.0",
            licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0"
        )
    )

    val availableChatbotModels: List<ModelInfo>
        get() = availableModelConfigs.filter { it.role == ModelAssetRole.CHATBOT }

    val availableEmbeddingModels: List<ModelInfo>
        get() = availableModelConfigs.filter { it.role == ModelAssetRole.EMBEDDING }

    fun isAnyEmbeddingModelReady(): Boolean =
        availableEmbeddingModels.any(::isModelReady)

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

    fun getActiveModelFile(preferredModelId: String? = null): File {
        val chatbotModels = availableModelConfigs.filter { it.role == ModelAssetRole.CHATBOT }
        val preferred = chatbotModels.firstOrNull { it.id == preferredModelId && isModelReady(it) }
        val model = preferred ?: chatbotModels.firstOrNull(::isModelReady)
            ?: error("Model LiteRT-LM belum tersedia")
        return getModelFile(model.filename)
    }

    fun getActiveModelInfo(preferredModelId: String? = null): ModelInfo? {
        val chatbotModels = availableModelConfigs.filter { it.role == ModelAssetRole.CHATBOT }
        val preferred = chatbotModels.firstOrNull { it.id == preferredModelId && isModelReady(it) }
        return preferred ?: chatbotModels.firstOrNull(::isModelReady)
    }

    fun getActiveEmbeddingModelFile(preferredModelId: String? = null): File? {
        val embeddingModels = availableModelConfigs.filter { it.role == ModelAssetRole.EMBEDDING }
        val preferred = embeddingModels.firstOrNull { it.id == preferredModelId && isModelReady(it) }
        val model = preferred ?: embeddingModels.firstOrNull(::isModelReady) ?: return null
        return getModelFile(model.filename)
    }

    fun getActiveEmbeddingModelInfo(preferredModelId: String? = null): ModelInfo? {
        val embeddingModels = availableModelConfigs.filter { it.role == ModelAssetRole.EMBEDDING }
        val preferred = embeddingModels.firstOrNull { it.id == preferredModelId && isModelReady(it) }
        return preferred ?: embeddingModels.firstOrNull(::isModelReady)
    }

    suspend fun resolveActiveEmbeddingModelInfo(preferredModelId: String? = null): ModelInfo? =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val embeddingModels = availableModelConfigs.filter { it.role == ModelAssetRole.EMBEDDING }
            val preferred = embeddingModels.firstOrNull { it.id == preferredModelId }
            if (preferred != null) {
                if (isModelReady(preferred) || verifyModelSha256Async(preferred)) return@withContext preferred
            }
            for (model in embeddingModels) {
                if (isModelReady(model) || verifyModelSha256Async(model)) return@withContext model
            }
            null
        }

    fun isAnyModelReady(): Boolean = availableModelConfigs
        .filter { it.role == ModelAssetRole.CHATBOT }
        .any(::isModelReady)

    suspend fun restoreVerifiedModelsFromSaf() =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            startupVerificationMutex.withLock {
                if (startupVerificationComplete) return@withLock
                // Prioritize embedding models first so RAG services become ready immediately
                (availableEmbeddingModels + availableChatbotModels)
                    .filter(ModelInfo::isDownloadable)
                    .forEach { model ->
                        // A process-local readiness bit is not durable trust.
                        // Hash every existing artifact once per startup, even
                        // when its byte length still matches the manifest.
                        if (!verifyModelSha256Async(model)) {
                            runCatching {
                                safAssetStore.materialize(
                                    relativePath = "models/${model.filename}",
                                    destination = getModelFile(model.filename),
                                    expectedSha256 = model.sha256.orEmpty()
                                )
                            }
                            verifyModelSha256Async(model)
                        }
                    }
                startupVerificationComplete = true
            }
        }

    suspend fun persistVerifiedModel(modelInfo: ModelInfo) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (!isModelReady(modelInfo)) return@withContext
        val publishedUri = safAssetStore.publishFile(
            source = getModelFile(modelInfo.filename),
            relativeDirectory = "models",
            filename = modelInfo.filename
        )
        check(
            safAssetStore.verifyFile(
                uri = publishedUri,
                expectedSizeBytes = modelInfo.sizeBytes ?: 0L,
                expectedSha256 = modelInfo.sha256.orEmpty()
            )
        ) { "Asset model di Folder SAF gagal diverifikasi" }
        safAssetStore.publishText(
            text = JSONObject()
                .put("id", modelInfo.id)
                .put("filename", modelInfo.filename)
                .put("size_bytes", modelInfo.sizeBytes)
                .put("sha256", modelInfo.sha256)
                .put("format", modelInfo.format)
                .put("runtime", modelInfo.runtime)
                .put("role", modelInfo.role.name)
                .put("tokenizer_asset", modelInfo.tokenizerAsset)
                .put("tokenizer_type", modelInfo.tokenizerType)
                .put("tokenizer_sha256", modelInfo.tokenizerSha256)
                .put("license_id", modelInfo.licenseId)
                .put("license_url", modelInfo.licenseUrl)
                .toString(),
            relativeDirectory = "manifests",
            filename = "model-${modelInfo.id}.json"
        )
    }

    companion object {
        internal val verifiedCache = java.util.concurrent.ConcurrentHashMap<String, Long>()
        private val startupVerificationMutex = Mutex()
        private var startupVerificationComplete = false

        @androidx.annotation.VisibleForTesting
        fun clearVerificationCache() {
            verifiedCache.clear()
            startupVerificationComplete = false
        }
    }

    fun isModelReady(modelInfo: ModelInfo): Boolean {
        if (!modelInfo.isDownloadable) return false
        val file = getModelFile(modelInfo.filename)
        if (!file.isFile) return false
        val expectedSize = modelInfo.sizeBytes ?: -1L
        if (expectedSize > 0L && file.length() != expectedSize) return false
        val cachedLastModified = verifiedCache[modelInfo.filename]
        if (cachedLastModified != null && cachedLastModified == file.lastModified()) {
            return true
        }
        return false
    }

    suspend fun verifyModelSha256Async(modelInfo: ModelInfo): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val file = getModelFile(modelInfo.filename)
        if (!file.isFile || file.length() != (modelInfo.sizeBytes ?: -1L)) {
            verifiedCache.remove(modelInfo.filename)
            return@withContext false
        }
        val isDigestValid = calculateSha256(file).equals(modelInfo.sha256, ignoreCase = true)
        if (isDigestValid) {
            verifiedCache[modelInfo.filename] = file.lastModified()
        } else {
            verifiedCache.remove(modelInfo.filename)
        }
        isDigestValid
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
