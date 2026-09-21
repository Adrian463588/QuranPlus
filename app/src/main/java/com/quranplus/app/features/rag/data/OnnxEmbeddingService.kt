package com.quranplus.app.features.rag.data

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import com.quranplus.app.features.chatbot.data.ModelAssetRole
import com.quranplus.app.features.chatbot.data.ModelRepository
import com.quranplus.app.features.rag.domain.RagIndexMetadata
import com.quranplus.app.features.settings.data.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

class EmbeddingModelUnavailable(message: String) : IllegalStateException(message)

data class EmbeddingContract(
    val modelId: String,
    val modelRevision: String,
    val tokenizerSha256: String,
    val dimension: Int,
    val normalized: Boolean,
    val pooling: String,
    val maxSequenceLength: Int
)

interface EmbeddingService {
    suspend fun embed(text: String): FloatArray
    suspend fun isReady(): Boolean = false

    suspend fun embeddingContract(): EmbeddingContract = EmbeddingContract(
        modelId = "unknown",
        modelRevision = "unknown",
        tokenizerSha256 = "unknown",
        dimension = 384,
        normalized = true,
        pooling = "mean",
        maxSequenceLength = 512
    )

    /** Number of content WordPiece tokens using the same tokenizer as embed(). */
    fun countContentTokens(text: String): Int = text.split(Regex("\\s+")).count(String::isNotBlank)
}

/**
 * Real ONNX Runtime embedder. Missing model/tokenizer is a blocked state.
 * No zero-vector or synthetic hash fallback is allowed.
 */
class OnnxEmbeddingService(
    private val context: Context,
    private val modelRepository: ModelRepository,
    private val preferencesManager: PreferencesManager? = null
) : EmbeddingService {

    private val environment by lazy { OrtEnvironment.getEnvironment() }
    private var currentSession: OrtSession? = null
    private var currentModelPath: String? = null
    private var activeTokenizer: RagTokenizer? = null
    private var activeTokenizerKey: String? = null
    private val sessionMutex = Mutex()
    private val inferenceMutex = Mutex()
    private val tokenizerMutex = Mutex()

    override suspend fun embed(text: String): FloatArray = inferenceMutex.withLock {
        withContext(Dispatchers.Default) {
            require(text.isNotBlank()) { "Embedding text must not be blank" }
            val tokenizer = getActiveTokenizer()
            val tokenIds = tokenizer.tokenize(text, MAX_SEQUENCE_LENGTH)
            val padId = tokenizer.padId
            val attention = LongArray(MAX_SEQUENCE_LENGTH) { index -> if (tokenIds[index] == padId) 0 else 1 }
            val tokenTypes = LongArray(MAX_SEQUENCE_LENGTH)
            val activeSession = getOrCreateSession()
            val inputNames = activeSession.inputNames

            OnnxTensor.createTensor(environment, arrayOf(tokenIds)).use { inputIds ->
                OnnxTensor.createTensor(environment, arrayOf(attention)).use { attentionMask ->
                    val inputs = mutableMapOf<String, OnnxTensor>()
                    inputs["input_ids"] = inputIds
                    if (inputNames.contains("attention_mask")) {
                        inputs["attention_mask"] = attentionMask
                    }
                    if (inputNames.contains("token_type_ids")) {
                        OnnxTensor.createTensor(environment, arrayOf(tokenTypes)).use { typeIds ->
                            inputs["token_type_ids"] = typeIds
                            activeSession.run(inputs).use { output ->
                                poolOutput(output[0].value, attention)
                            }
                        }
                    } else {
                        activeSession.run(inputs).use { output ->
                            poolOutput(output[0].value, attention)
                        }
                    }
                }
            }
        }
    }

    override suspend fun isReady(): Boolean = withContext(Dispatchers.IO) {
        findModel() != null
    }

    override suspend fun embeddingContract(): EmbeddingContract = withContext(Dispatchers.IO) {
        val model = modelRepository.resolveActiveEmbeddingModelInfo(
            preferencesManager?.selectedEmbeddingModel?.firstOrNull()
        ) ?: throw EmbeddingModelUnavailable("Embedding model belum tersedia")
        EmbeddingContract(
            modelId = model.id,
            modelRevision = revisionFromUrl(model.artifactUrl),
            tokenizerSha256 = model.tokenizerSha256 ?: readTokenizerSha256(),
            dimension = model.embeddingDimension ?: 384,
            normalized = true,
            pooling = "mean",
            maxSequenceLength = MAX_SEQUENCE_LENGTH
        )
    }

    override fun countContentTokens(text: String): Int {
        val tokenizer = activeTokenizer ?: runCatching {
            val vocab = ConcurrentHashMap<String, Long>()
            context.assets.open(VOCABULARY_ASSET).bufferedReader().useLines { lines ->
                lines.forEachIndexed { index, token -> vocab[token.trim()] = index.toLong() }
            }
            WordPieceTokenizer(vocab)
        }.getOrNull()
        return tokenizer?.countContentTokens(text) ?: text.split(Regex("\\s+")).count(String::isNotBlank)
    }

    private suspend fun getOrCreateSession(): OrtSession = sessionMutex.withLock {
        val model = findModel()
            ?: throw EmbeddingModelUnavailable(
                "ONNX embedding model unavailable. Download or select an embedding model first."
            )
        if (model.length() == 0L) throw EmbeddingModelUnavailable("ONNX embedding model is empty")

        if (currentSession == null || currentModelPath != model.absolutePath) {
            currentSession?.close()
            val options = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(2)
                setInterOpNumThreads(1)
            }
            currentSession = environment.createSession(model.absolutePath, options)
            currentModelPath = model.absolutePath
        }
        currentSession!!
    }

    private suspend fun getActiveTokenizer(): RagTokenizer = tokenizerMutex.withLock {
        val preferredId = preferencesManager?.selectedEmbeddingModel?.firstOrNull()
        val modelInfo = modelRepository.resolveActiveEmbeddingModelInfo(preferredId)
            ?: throw EmbeddingModelUnavailable("Embedding model belum tersedia")
        val key = "${modelInfo.id}_${modelInfo.tokenizerType}_${modelInfo.tokenizerAsset}"
        if (activeTokenizer != null && activeTokenizerKey == key) {
            return@withLock activeTokenizer!!
        }

        val assetPath = modelInfo.tokenizerAsset
            ?: throw EmbeddingModelUnavailable("Tokenizer asset belum dikonfigurasi untuk model ${modelInfo.id}")
        val expectedSha = modelInfo.tokenizerSha256
            ?: throw EmbeddingModelUnavailable("Tokenizer SHA-256 belum dikonfigurasi untuk model ${modelInfo.id}")

        verifyAssetHash(assetPath, expectedSha)

        val tokenizer: RagTokenizer = when (modelInfo.tokenizerType?.lowercase()) {
            "sentencepiece" -> {
                context.assets.open(assetPath).use { stream ->
                    SentencePieceTokenizer(stream)
                }
            }
            "wordpiece" -> {
                val vocab = ConcurrentHashMap<String, Long>()
                context.assets.open(assetPath).bufferedReader().useLines { lines ->
                    lines.forEachIndexed { index, token -> vocab[token.trim()] = index.toLong() }
                }
                WordPieceTokenizer(vocab)
            }
            else -> throw EmbeddingModelUnavailable("Jenis tokenizer '${modelInfo.tokenizerType}' belum didukung")
        }

        activeTokenizer = tokenizer
        activeTokenizerKey = key
        tokenizer
    }

    private suspend fun findModel(): File? {
        val preferredId = preferencesManager?.selectedEmbeddingModel?.firstOrNull()
        val modelInfo = modelRepository.resolveActiveEmbeddingModelInfo(preferredId)
            ?: return null
        val assetPath = modelInfo.tokenizerAsset ?: return null
        val expectedSha = modelInfo.tokenizerSha256 ?: return null
        val type = modelInfo.tokenizerType?.lowercase()
        if (type != "wordpiece" && type != "sentencepiece") return null
        if (!runCatching { verifyAssetHash(assetPath, expectedSha) }.isSuccess) return null
        val file = modelRepository.getModelFile(modelInfo.filename)
        return file.takeIf { it.exists() && it.length() > 0L }
    }

    private fun verifyAssetHash(assetPath: String, expectedSha: String) {
        if (!expectedSha.matches(SHA256_PATTERN)) {
            throw EmbeddingModelUnavailable("Embedding tokenizer manifest is invalid for $assetPath")
        }
        val digest = MessageDigest.getInstance("SHA-256")
        context.assets.open(assetPath).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        if (!actual.equals(expectedSha, ignoreCase = true)) {
            throw EmbeddingModelUnavailable("Embedding tokenizer SHA-256 mismatch for $assetPath")
        }
    }

    private fun readTokenizerSha256(): String =
        context.assets.open(VOCABULARY_HASH_ASSET).bufferedReader().use { it.readText().trim() }

    private fun revisionFromUrl(url: String): String =
        Regex("/resolve/([0-9a-fA-F]{40})/").find(url)?.groupValues?.get(1).orEmpty()

    private fun poolOutput(value: Any, attentionMask: LongArray): FloatArray {
        val tokenRows: Array<FloatArray> = when (value) {
            is Array<*> -> when {
                value.firstOrNull() is Array<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    (value[0] as Array<FloatArray>)
                }
                value.firstOrNull() is FloatArray -> {
                    @Suppress("UNCHECKED_CAST")
                    value as Array<FloatArray>
                }
                else -> throw EmbeddingModelUnavailable("Unsupported ONNX output shape")
            }
            else -> throw EmbeddingModelUnavailable("Unsupported ONNX output type")
        }

        val dimension = tokenRows.firstOrNull()?.size
            ?: throw EmbeddingModelUnavailable("ONNX output has no embedding rows")
        if (dimension != EMBEDDING_DIMENSION) {
            throw EmbeddingModelUnavailable(
                "ONNX embedding dimension $dimension does not match $EMBEDDING_DIMENSION"
            )
        }
        val pooled = FloatArray(dimension)
        var count = 0
        tokenRows.forEachIndexed { index, row ->
            if (index < attentionMask.size && attentionMask[index] == 1L) {
                row.forEachIndexed { dimensionIndex, component -> pooled[dimensionIndex] += component }
                count++
            }
        }
        if (count == 0) throw EmbeddingModelUnavailable("ONNX output contains no active tokens")
        pooled.forEachIndexed { index, component -> pooled[index] = component / count }
        val norm = kotlin.math.sqrt(pooled.sumOf { (it * it).toDouble() }).toFloat()
        if (!norm.isFinite() || norm == 0f) throw EmbeddingModelUnavailable("ONNX embedding is invalid")
        pooled.forEachIndexed { index, component -> pooled[index] = component / norm }
        return pooled
    }

    private companion object {
        const val MAX_SEQUENCE_LENGTH = 512
        const val PAD_ID = 0L
        const val CLS_ID = 101L
        const val SEP_ID = 102L
        const val UNKNOWN_ID = 100L
        const val CLS_TOKEN = "[CLS]"
        const val SEP_TOKEN = "[SEP]"
        const val UNKNOWN_TOKEN = "[UNK]"
        const val VOCABULARY_ASSET = "embedding/vocab.txt"
        const val VOCABULARY_HASH_ASSET = "embedding/vocab.txt.sha256"
        const val EMBEDDING_DIMENSION = 384
        val SHA256_PATTERN = Regex("[0-9a-fA-F]{64}")
    }
}
