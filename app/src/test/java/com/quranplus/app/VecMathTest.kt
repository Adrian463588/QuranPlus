package com.quranplus.app

import com.quranplus.app.core.utils.VecMath
import com.quranplus.app.core.utils.VecMath.toByteArray
import com.quranplus.app.core.utils.VecMath.toFloatArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VecMathTest {

    @Test
    fun GIVEN_floatArray_WHEN_convertedToByteArrayAndBack_THEN_valuesMatch() {
        val original = floatArrayOf(0.123f, -0.456f, 0.789f, 1.0f, -1.0f)
        val byteArray = original.toByteArray()
        val restored = byteArray.toFloatArray()

        assertEquals(original.size, restored.size)
        for (i in original.indices) {
            assertEquals(original[i], restored[i], 1e-6f)
        }
    }

    @Test
    fun GIVEN_identicalVectors_WHEN_computingCosineSimilarity_THEN_scoreIsOne() {
        val v1 = floatArrayOf(1f, 2f, 3f)
        val v2 = floatArrayOf(1f, 2f, 3f)
        val similarity = VecMath.cosineSimilarity(v1, v2)

        assertEquals(1.0f, similarity, 1e-4f)
    }

    @Test
    fun GIVEN_orthogonalVectors_WHEN_computingCosineSimilarity_THEN_scoreIsZero() {
        val v1 = floatArrayOf(1f, 0f, 0f)
        val v2 = floatArrayOf(0f, 1f, 0f)
        val similarity = VecMath.cosineSimilarity(v1, v2)

        assertEquals(0.0f, similarity, 1e-4f)
    }

    @Test
    fun GIVEN_rawVector_WHEN_normalized_THEN_magnitudeIsOne() {
        val raw = floatArrayOf(3f, 4f)
        val normalized = VecMath.l2Normalize(raw)

        var magSq = 0f
        for (v in normalized) magSq += v * v
        assertEquals(1.0f, kotlin.math.sqrt(magSq), 1e-4f)
    }
}
