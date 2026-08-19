package com.quranplus.app.features.rag.data

import android.content.Context
import com.quranplus.app.features.rag.domain.VectorIndex
import com.quranplus.app.features.rag.domain.VectorRecord

/**
 * sqlite-vec boundary. The Android native library and pinned checksum are
 * intentionally absent from this build, so no Room scan can masquerade as a
 * vector index. This class becomes ready only after the verified native/index
 * asset is added and its self-test is implemented.
 */
class SqliteVecVectorIndex(
    private val context: Context
) : VectorIndex {
    override suspend fun isReady(): Boolean = false

    override suspend fun replace(records: List<VectorRecord>): Int {
        throw VectorIndexUnavailable(
            "sqlite-vec index unavailable; verified native library and index asset are required"
        )
    }
}
