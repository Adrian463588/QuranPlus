package com.quranplus.app.features.rag.data

import androidx.room.PooledConnection
import androidx.room.Transactor
import androidx.room.useReaderConnection
import androidx.room.useWriterConnection
import com.quranplus.app.core.database.QuranDatabase
import com.quranplus.app.features.rag.domain.VectorIndex
import com.quranplus.app.features.rag.domain.VectorIndexCoverage
import com.quranplus.app.features.rag.domain.VectorMatch
import com.quranplus.app.features.rag.domain.VectorRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder

class VectorIndexUnavailable(message: String) : IllegalStateException(message)

/** sqlite-vec backed index; no text or Room scan is used for retrieval. */
class SqliteVecVectorIndex(
    private val database: QuranDatabase
) : VectorIndex {

    override suspend fun isReady(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            database.useWriterConnection { connection ->
                isReady(connection)
            }
        }.getOrDefault(false)
    }

    override suspend fun coverage(): VectorIndexCoverage = withContext(Dispatchers.IO) {
        runCatching {
            if (!isReady()) return@runCatching VectorIndexCoverage(0, emptySet())
            database.useReaderConnection { connection ->
                connection.usePrepared(COVERAGE_SQL) { statement ->
                    var recordCount = 0
                    val sourceTypes = linkedSetOf<String>()
                    while (statement.step()) {
                        sourceTypes += statement.getText(0)
                        recordCount += statement.getInt(1)
                    }
                    VectorIndexCoverage(recordCount, sourceTypes)
                }
            }
        }.getOrDefault(VectorIndexCoverage(0, emptySet()))
    }

    override suspend fun replace(records: List<VectorRecord>): Int = withContext(Dispatchers.IO) {
        require(records.isNotEmpty()) { "Tidak ada record untuk di-index" }
        require(records.all { it.embedding.size == EMBEDDING_DIMENSION }) {
            "Semua embedding harus berukuran $EMBEDDING_DIMENSION"
        }

        database.useWriterConnection { connection ->
            if (!isReady(connection)) {
                throw VectorIndexUnavailable("sqlite-vec belum tersedia pada ABI ini")
            }
            connection.withTransaction(Transactor.SQLiteTransactionType.IMMEDIATE) {
                usePrepared("DELETE FROM $TABLE_NAME") { statement -> statement.step() }
                records.forEach { record -> insert(this, record) }
                records.size
            }
        }
    }

    override suspend fun search(
        queryEmbedding: FloatArray,
        k: Int
    ): List<VectorMatch> = withContext(Dispatchers.IO) {
        require(queryEmbedding.size == EMBEDDING_DIMENSION) {
            "Query embedding harus berukuran $EMBEDDING_DIMENSION"
        }
        require(k > 0) { "k harus positif" }

        database.useReaderConnection { connection ->
            if (!hasVectorExtension(connection)) {
                throw VectorIndexUnavailable("sqlite-vec belum tersedia")
            }
            connection.usePrepared(SEARCH_SQL) { statement ->
                statement.bindBlob(1, floatBytes(queryEmbedding))
                statement.bindLong(2, k.toLong())
                val results = ArrayList<VectorMatch>(k)
                while (statement.step()) {
                    results += VectorMatch(
                        sourceId = statement.getText(0),
                        sourceType = statement.getText(1),
                        collectionId = statement.getText(2),
                        title = statement.getText(3),
                        reference = statement.getText(4),
                        identifier = statement.getText(5),
                        text = statement.getText(6),
                        distance = statement.getFloat(7),
                        surahNumber = statement.intOrNull(8),
                        ayahNumber = statement.intOrNull(9)
                    )
                }
                results
            }
        }
    }

    private suspend fun isReady(connection: Transactor): Boolean {
        if (!hasVectorExtension(connection)) return false
        ensureTable(connection)
        return true
    }

    private suspend fun hasVectorExtension(connection: PooledConnection): Boolean =
        connection.usePrepared("SELECT vec_version()") { statement ->
            statement.step() && statement.getText(0).isNotBlank()
        }

    private suspend fun ensureTable(connection: PooledConnection) {
        connection.usePrepared(CREATE_TABLE_SQL) { statement -> statement.step() }
    }

    private suspend fun insert(connection: PooledConnection, record: VectorRecord) {
        connection.usePrepared(INSERT_SQL) { statement ->
            statement.bindBlob(1, floatBytes(record.embedding))
            statement.bindText(2, record.sourceType)
            statement.bindText(3, record.collectionId)
            statement.bindLong(4, record.chunkIndex.toLong())
            statement.bindText(5, record.sourceId)
            statement.bindText(6, record.title)
            statement.bindText(7, record.reference)
            statement.bindText(8, record.identifier)
            statement.bindText(9, record.text)
            record.surahNumber.bindTo(statement, 10)
            record.ayahNumber.bindTo(statement, 11)
            statement.step()
        }
    }

    private fun Int?.bindTo(statement: androidx.sqlite.SQLiteStatement, index: Int) {
        if (this == null) statement.bindNull(index) else statement.bindLong(index, toLong())
    }

    private fun androidx.sqlite.SQLiteStatement.intOrNull(index: Int): Int? =
        if (isNull(index)) null else getInt(index)

    private fun floatBytes(values: FloatArray): ByteArray =
        ByteBuffer.allocate(values.size * Float.SIZE_BYTES)
            .order(ByteOrder.LITTLE_ENDIAN)
            .apply { values.forEach(::putFloat) }
            .array()

    private companion object {
        const val TABLE_NAME = "quranplus_vectors"
        const val EMBEDDING_DIMENSION = 384
        const val CREATE_TABLE_SQL = """
            CREATE VIRTUAL TABLE IF NOT EXISTS quranplus_vectors USING vec0(
                embedding float[384] distance_metric=cosine,
                source_type TEXT,
                collection_id TEXT,
                chunk_index INTEGER,
                +source_id TEXT,
                +title TEXT,
                +reference TEXT,
                +identifier TEXT,
                +text_content TEXT,
                +surah_number INTEGER,
                +ayah_number INTEGER
            )
        """
        const val INSERT_SQL = """
            INSERT INTO quranplus_vectors(
                embedding, source_type, collection_id, chunk_index, source_id,
                title, reference, identifier, text_content, surah_number, ayah_number
            ) VALUES (vec_f32(?), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """
        const val SEARCH_SQL = """
            SELECT source_id, source_type, collection_id, title, reference,
                identifier, text_content, distance, surah_number, ayah_number
            FROM quranplus_vectors
            WHERE embedding MATCH vec_f32(?) AND k = ?
        """
        const val COVERAGE_SQL = """
            SELECT source_type, COUNT(*)
            FROM quranplus_vectors
            GROUP BY source_type
        """
    }
}
