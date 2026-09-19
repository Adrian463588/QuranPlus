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
import com.quranplus.app.features.rag.domain.RagIndexMetadata
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
                    val recordCounts = linkedMapOf<String, Int>()
                    while (statement.step()) {
                        val sourceType = statement.getText(0)
                        val sourceCount = statement.getInt(1)
                        recordCounts[sourceType] = sourceCount
                        recordCount += sourceCount
                    }
                    VectorIndexCoverage(
                        recordCount = recordCount,
                        sourceTypes = recordCounts.keys,
                        recordCountsBySourceType = recordCounts
                    )
                }
            }
        }.getOrDefault(VectorIndexCoverage(0, emptySet()))
    }

    override suspend fun metadata(): RagIndexMetadata? = withContext(Dispatchers.IO) {
        runCatching {
            database.useReaderConnection { connection ->
                if (!hasVectorExtension(connection)) return@useReaderConnection null
                connection.usePrepared(METADATA_SELECT_SQL) { statement ->
                    if (!statement.step()) return@usePrepared null
                    RagIndexMetadata(
                        fingerprint = statement.getText(0),
                        modelId = statement.getText(1),
                        modelRevision = statement.getText(2),
                        tokenizerSha256 = statement.getText(3),
                        embeddingDimension = statement.getInt(4),
                        normalized = statement.getInt(5) == 1,
                        pooling = statement.getText(6),
                        maxSequenceLength = statement.getInt(7),
                        chunkTokenCount = statement.getInt(8),
                        chunkOverlapTokens = statement.getInt(9),
                        corpusRecordCount = statement.getInt(10),
                        corpusFingerprint = statement.getText(11),
                        updatedAt = statement.getLong(12)
                    )
                }
            }
        }.getOrNull()
    }

    override suspend fun replace(records: List<VectorRecord>): Int = withContext(Dispatchers.IO) {
        replaceInternal(records, null)
    }

    override suspend fun replace(
        records: List<VectorRecord>,
        metadata: RagIndexMetadata
    ): Int = withContext(Dispatchers.IO) {
        replaceInternal(records, metadata)
    }

    private suspend fun replaceInternal(
        records: List<VectorRecord>,
        metadata: RagIndexMetadata?
    ): Int {
        require(records.isNotEmpty()) { "Tidak ada record untuk di-index" }
        require(records.all { it.embedding.size == EMBEDDING_DIMENSION }) {
            "Semua embedding harus berukuran $EMBEDDING_DIMENSION"
        }

        return database.useWriterConnection { connection ->
            if (!isReady(connection)) {
                throw VectorIndexUnavailable("sqlite-vec belum tersedia pada ABI ini")
            }
            connection.withTransaction(Transactor.SQLiteTransactionType.IMMEDIATE) {
                usePrepared("DELETE FROM $TABLE_NAME") { statement -> statement.step() }
                records.forEach { record -> insert(this, record) }
                metadata?.let { writeMetadata(this, it) }
                    ?: usePrepared("DELETE FROM rag_index_metadata") { statement -> statement.step() }
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
        connection.usePrepared(CREATE_METADATA_TABLE_SQL) { statement -> statement.step() }
    }

    private suspend fun writeMetadata(
        connection: PooledConnection,
        metadata: RagIndexMetadata
    ) {
        connection.usePrepared(METADATA_UPSERT_SQL) { statement ->
            statement.bindText(1, metadata.fingerprint)
            statement.bindText(2, metadata.modelId)
            statement.bindText(3, metadata.modelRevision)
            statement.bindText(4, metadata.tokenizerSha256)
            statement.bindLong(5, metadata.embeddingDimension.toLong())
            statement.bindLong(6, if (metadata.normalized) 1L else 0L)
            statement.bindText(7, metadata.pooling)
            statement.bindLong(8, metadata.maxSequenceLength.toLong())
            statement.bindLong(9, metadata.chunkTokenCount.toLong())
            statement.bindLong(10, metadata.chunkOverlapTokens.toLong())
            statement.bindLong(11, metadata.corpusRecordCount.toLong())
            statement.bindText(12, metadata.corpusFingerprint)
            statement.bindLong(13, metadata.updatedAt)
            statement.step()
        }
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
        const val CREATE_METADATA_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS rag_index_metadata(
                id INTEGER PRIMARY KEY CHECK(id = 1),
                fingerprint TEXT NOT NULL,
                model_id TEXT NOT NULL,
                model_revision TEXT NOT NULL,
                tokenizer_sha256 TEXT NOT NULL,
                embedding_dimension INTEGER NOT NULL,
                normalized INTEGER NOT NULL,
                pooling TEXT NOT NULL,
                max_sequence_length INTEGER NOT NULL,
                chunk_token_count INTEGER NOT NULL,
                chunk_overlap_tokens INTEGER NOT NULL,
                corpus_record_count INTEGER NOT NULL,
                corpus_fingerprint TEXT NOT NULL,
                updated_at INTEGER NOT NULL
            )
        """
        const val METADATA_SELECT_SQL = """
            SELECT fingerprint, model_id, model_revision, tokenizer_sha256,
                embedding_dimension, normalized, pooling, max_sequence_length,
                chunk_token_count, chunk_overlap_tokens, corpus_record_count,
                corpus_fingerprint, updated_at
            FROM rag_index_metadata
            WHERE id = 1
        """
        const val METADATA_UPSERT_SQL = """
            INSERT INTO rag_index_metadata(
                id, fingerprint, model_id, model_revision, tokenizer_sha256,
                embedding_dimension, normalized, pooling, max_sequence_length,
                chunk_token_count, chunk_overlap_tokens, corpus_record_count,
                corpus_fingerprint, updated_at
            ) VALUES (
                1, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
            )
            ON CONFLICT(id) DO UPDATE SET
                fingerprint = excluded.fingerprint,
                model_id = excluded.model_id,
                model_revision = excluded.model_revision,
                tokenizer_sha256 = excluded.tokenizer_sha256,
                embedding_dimension = excluded.embedding_dimension,
                normalized = excluded.normalized,
                pooling = excluded.pooling,
                max_sequence_length = excluded.max_sequence_length,
                chunk_token_count = excluded.chunk_token_count,
                chunk_overlap_tokens = excluded.chunk_overlap_tokens,
                corpus_record_count = excluded.corpus_record_count,
                corpus_fingerprint = excluded.corpus_fingerprint,
                updated_at = excluded.updated_at
        """
    }
}
