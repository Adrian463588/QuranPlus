package com.quranplus.app.features.rag.data

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

class EmbeddingModelUnavailable(message: String) : IllegalStateException(message)

interface EmbeddingService {
    suspend fun embed(text: String): FloatArray
    suspend fun isReady(): Boolean = false
}

/**
 * Real ONNX Runtime embedder. Missing model/tokenizer is a blocked state.
 * No zero-vector or synthetic hash fallback is allowed.
 */
class OnnxEmbeddingService(
    private val context: Context
) : EmbeddingService {

    private val environment by lazy { OrtEnvironment.getEnvironment() }
    private val vocabulary by lazy { loadVocabulary() }
    private val session by lazy { createSession() }

    override suspend fun embed(text: String): FloatArray = withContext(Dispatchers.Default) {
        require(text.isNotBlank()) { "Embedding text must not be blank" }
        val tokenIds = tokenize(text)
        val attention = LongArray(MAX_SEQUENCE_LENGTH) { index -> if (tokenIds[index] == PAD_ID) 0 else 1 }
        val tokenTypes = LongArray(MAX_SEQUENCE_LENGTH)

        OnnxTensor.createTensor(environment, arrayOf(tokenIds)).use { inputIds ->
            OnnxTensor.createTensor(environment, arrayOf(attention)).use { attentionMask ->
                OnnxTensor.createTensor(environment, arrayOf(tokenTypes)).use { typeIds ->
                    val inputs = mapOf(
                        "input_ids" to inputIds,
                        "attention_mask" to attentionMask,
                        "token_type_ids" to typeIds
                    )
                    session.run(inputs).use { output ->
                        poolOutput(output[0].value, attention)
                    }
                }
            }
        }
    }

    override suspend fun isReady(): Boolean = withContext(Dispatchers.IO) {
        findModel() != null && runCatching { verifyVocabularyHash() }.isSuccess
    }

    private fun createSession(): OrtSession {
        val model = findModel()
            ?: throw EmbeddingModelUnavailable(
                "ONNX embedding model unavailable. Import a verified all-MiniLM-L6-v2 ONNX model first."
            )
        if (model.length() == 0L) throw EmbeddingModelUnavailable("ONNX embedding model is empty")

        val options = OrtSession.SessionOptions().apply {
            setIntraOpNumThreads(2)
            setInterOpNumThreads(1)
        }
        return environment.createSession(model.absolutePath, options)
    }

    private fun findModel(): File? {
        val candidates = listOf(File(context.filesDir, "models/all-MiniLM-L6-v2.onnx"))
        return candidates.firstOrNull { candidate ->
            val digestFile = File(candidate.parentFile, "${candidate.name}.sha256")
            candidate.isFile && digestFile.isFile &&
                digestFile.readText().trim().matches(SHA256_PATTERN) &&
                calculateSha256(candidate).equals(digestFile.readText().trim(), ignoreCase = true)
        }
    }

    private fun loadVocabulary(): Map<String, Long> {
        verifyVocabularyHash()
        val result = ConcurrentHashMap<String, Long>()
        context.assets.open("embedding/vocab.txt").bufferedReader().useLines { lines ->
            lines.forEachIndexed { index, token -> result[token.trim()] = index.toLong() }
        }
        return result
    }

    private fun verifyVocabularyHash() {
        val expected = context.assets.open(VOCABULARY_HASH_ASSET).bufferedReader().use { it.readText().trim() }
        if (!expected.matches(SHA256_PATTERN)) {
            throw EmbeddingModelUnavailable("Embedding tokenizer manifest is invalid")
        }
        val digest = MessageDigest.getInstance("SHA-256")
        context.assets.open(VOCABULARY_ASSET).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        if (!actual.equals(expected, ignoreCase = true)) {
            throw EmbeddingModelUnavailable("Embedding tokenizer SHA-256 mismatch")
        }
    }

    private fun tokenize(text: String): LongArray {
        val tokens = ArrayList<Long>(MAX_SEQUENCE_LENGTH)
        tokens += vocabulary[CLS_TOKEN] ?: CLS_ID
        splitOnWhitespaceAndPunctuation(text.lowercase())
            .flatMap(::wordPiece)
            .take(MAX_SEQUENCE_LENGTH - 2)
            .forEach { token ->
                tokens += vocabulary[token] ?: (vocabulary[UNKNOWN_TOKEN] ?: UNKNOWN_ID)
            }
        tokens += vocabulary[SEP_TOKEN] ?: SEP_ID
        while (tokens.size < MAX_SEQUENCE_LENGTH) tokens += PAD_ID
        return tokens.take(MAX_SEQUENCE_LENGTH).toLongArray()
    }

    private fun splitOnWhitespaceAndPunctuation(text: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        fun flush() {
            if (current.isNotEmpty()) {
                result += current.toString()
                current.clear()
            }
        }
        text.forEach { character ->
            if (character.isWhitespace() || isAsciiPunctuation(character)) {
                flush()
                if (!character.isWhitespace()) result += character.toString()
            } else {
                current.append(character)
            }
        }
        flush()
        return result
    }

    private fun wordPiece(word: String): List<String> {
        if (word.isBlank()) return emptyList()
        val pieces = mutableListOf<String>()
        var start = 0
        while (start < word.length) {
            var end = word.length
            var match: String? = null
            while (start < end) {
                val candidate = if (start == 0) word.substring(0, end) else "##${word.substring(start, end)}"
                if (candidate in vocabulary) {
                    match = candidate
                    break
                }
                end--
            }
            if (match == null) return listOf(UNKNOWN_TOKEN)
            pieces += match
            start = end
        }
        return pieces
    }

    private fun isAsciiPunctuation(character: Char): Boolean {
        val code = character.code
        return code in 33..47 || code in 58..64 || code in 91..96 || code in 123..126
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
