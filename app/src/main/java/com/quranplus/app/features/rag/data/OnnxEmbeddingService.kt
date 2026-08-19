package com.quranplus.app.features.rag.data

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.LongBuffer
import java.util.concurrent.ConcurrentHashMap

class EmbeddingModelUnavailable(message: String) : IllegalStateException(message)

interface EmbeddingService {
    suspend fun embed(text: String): FloatArray
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

        OnnxTensor.createTensor(environment, LongBuffer.wrap(tokenIds)).use { inputIds ->
            OnnxTensor.createTensor(environment, LongBuffer.wrap(attention)).use { attentionMask ->
                OnnxTensor.createTensor(environment, LongBuffer.wrap(tokenTypes)).use { typeIds ->
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
        val candidates = listOf(
            File(context.filesDir, "models/all-MiniLM-L6-v2.onnx"),
            File(context.getExternalFilesDir(null), "models/all-MiniLM-L6-v2.onnx")
        )
        return candidates.firstOrNull { it.isFile && it.length() > 0L }
    }

    private fun loadVocabulary(): Map<String, Long> {
        val result = ConcurrentHashMap<String, Long>()
        context.assets.open("embedding/vocab.txt").bufferedReader().useLines { lines ->
            lines.forEachIndexed { index, token -> result[token.trim()] = index.toLong() }
        }
        return result
    }

    private fun tokenize(text: String): LongArray {
        val tokens = ArrayList<Long>(MAX_SEQUENCE_LENGTH)
        tokens += vocabulary[CLS_TOKEN] ?: CLS_ID
        text.lowercase()
            .split(Regex("[^\\p{L}\\p{M}\\p{N}]+"))
            .filter(String::isNotBlank)
            .forEach { word ->
                tokens += vocabulary[word] ?: (vocabulary[UNKNOWN_TOKEN] ?: UNKNOWN_ID)
            }
        tokens += vocabulary[SEP_TOKEN] ?: SEP_ID
        while (tokens.size < MAX_SEQUENCE_LENGTH) tokens += PAD_ID
        return tokens.take(MAX_SEQUENCE_LENGTH).toLongArray()
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
        const val MAX_SEQUENCE_LENGTH = 128
        const val PAD_ID = 0L
        const val CLS_ID = 101L
        const val SEP_ID = 102L
        const val UNKNOWN_ID = 100L
        const val CLS_TOKEN = "[CLS]"
        const val SEP_TOKEN = "[SEP]"
        const val UNKNOWN_TOKEN = "[UNK]"
    }
}
