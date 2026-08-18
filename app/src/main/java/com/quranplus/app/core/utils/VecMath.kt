package com.quranplus.app.core.utils

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

/**
 * High-performance Vector & Embedding Math Utilities for RAG
 */
object VecMath {

    fun FloatArray.toByteArray(): ByteArray {
        val buf = ByteBuffer.allocate(size * Float.SIZE_BYTES).order(ByteOrder.nativeOrder())
        for (f in this) buf.putFloat(f)
        return buf.array()
    }

    fun ByteArray.toFloatArray(): FloatArray {
        val buf = ByteBuffer.wrap(this).order(ByteOrder.nativeOrder())
        return FloatArray(size / Float.SIZE_BYTES) { buf.getFloat() }
    }

    /**
     * Computes Cosine Similarity between two normalized or raw embedding vectors
     */
    fun cosineSimilarity(v1: FloatArray, v2: FloatArray): Float {
        if (v1.isEmpty() || v2.isEmpty() || v1.size != v2.size) return 0f
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in v1.indices) {
            val a = v1[i]
            val b = v2[i]
            dot += a * b
            normA += a * a
            normB += b * b
        }
        val denom = (sqrt(normA) * sqrt(normB)).coerceAtLeast(1e-9f)
        return dot / denom
    }

    /**
     * In-place or copy L2 Normalization
     */
    fun l2Normalize(vec: FloatArray): FloatArray {
        var normSq = 0f
        for (v in vec) normSq += v * v
        val norm = sqrt(normSq).coerceAtLeast(1e-9f)
        return FloatArray(vec.size) { vec[it] / norm }
    }
}
