package com.quranplus.app.features.rag.data

import android.content.Context
import com.quranplus.app.core.utils.VecMath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

interface EmbeddingService {
    suspend fun embed(text: String): FloatArray
}

class TfLiteEmbeddingService(private val context: Context) : EmbeddingService {

    private val vocab: Map<String, Int> by lazy { loadVocab() }

    private val interpreter: Interpreter? by lazy {
        try {
            val modelBuffer = loadModelFile("embedding/all-MiniLM-L6-v2.tflite")
            val options = Interpreter.Options().apply {
                setNumThreads(2) // Strictly limited to 2 threads per PRD to avoid thermal throttling
            }
            Interpreter(modelBuffer, options)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun embed(text: String): FloatArray = withContext(Dispatchers.Default) {
        val interp = interpreter ?: return@withContext FloatArray(EMBEDDING_DIM)

        val inputIds = tokenize(text)
        val attentionMask = maskFor(inputIds)
        val output = Array(1) { FloatArray(EMBEDDING_DIM) }

        try {
            interp.runForMultipleInputsOutputs(
                arrayOf(inputIds, attentionMask),
                mapOf(0 to output)
            )
            VecMath.l2Normalize(output[0])
        } catch (e: Exception) {
            // Fallback for character-level hashing or zero-vector
            val fallback = FloatArray(EMBEDDING_DIM)
            val words = text.lowercase().split("\\s+".toRegex())
            for ((idx, word) in words.withIndex()) {
                val pos = (word.hashCode().let { if (it < 0) -it else it }) % EMBEDDING_DIM
                fallback[pos] += 1.0f / (idx + 1)
            }
            VecMath.l2Normalize(fallback)
        }
    }

    private fun tokenize(text: String): Array<IntArray> {
        val clsId = vocab["[CLS]"] ?: 101
        val sepId = vocab["[SEP]"] ?: 102
        val unkId = vocab["[UNK]"] ?: 100

        val tokens = mutableListOf(clsId)
        text.lowercase().split(Regex("[\\s\\p{Punct}]+")).forEach { word ->
            if (word.isNotEmpty()) {
                tokens.add(vocab[word] ?: unkId)
            }
        }
        tokens.add(sepId)

        while (tokens.size < MAX_SEQ_LEN) tokens.add(0)
        return arrayOf(tokens.take(MAX_SEQ_LEN).toIntArray())
    }

    private fun maskFor(inputIds: Array<IntArray>): Array<IntArray> =
        arrayOf(IntArray(inputIds[0].size) { if (inputIds[0][it] != 0) 1 else 0 })

    private fun loadVocab(): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        try {
            context.assets.open("embedding/vocab.txt").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).useLines { lines ->
                    lines.forEachIndexed { index, line ->
                        map[line.trim()] = index
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return map
    }

    private fun loadModelFile(path: String): ByteBuffer {
        val fileDescriptor = context.assets.openFd(path)
        val inputStream = fileDescriptor.createInputStream()
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength).order(ByteOrder.nativeOrder())
    }

    companion object {
        private const val MAX_SEQ_LEN = 128
        const val EMBEDDING_DIM = 384
    }
}
