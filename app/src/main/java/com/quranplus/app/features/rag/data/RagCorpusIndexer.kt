package com.quranplus.app.features.rag.data

import com.quranplus.app.core.database.QuranDatabase
import com.quranplus.app.features.rag.domain.IndexCorpusResult
import com.quranplus.app.features.rag.domain.VectorIndex
import com.quranplus.app.features.rag.domain.VectorRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Builds one index from the installed Quran, imported Hadith, and RAG documents. */
class RagCorpusIndexer(
    private val database: QuranDatabase,
    private val embeddingService: EmbeddingService,
    private val vectorIndex: VectorIndex
) {
    suspend fun index(): IndexCorpusResult = withContext(Dispatchers.Default) {
        if (!embeddingService.isReady()) return@withContext IndexCorpusResult.Blocked("EMBEDDER_UNAVAILABLE")
        if (!vectorIndex.isReady()) return@withContext IndexCorpusResult.Blocked("INDEX_UNAVAILABLE")

        val records = buildRecords()
        if (records.isEmpty()) return@withContext IndexCorpusResult.Blocked("CORPUS_UNAVAILABLE")

        val embedded = ArrayList<VectorRecord>(records.size)
        records.forEach { record ->
            embedded += record.copy(embedding = embeddingService.embed(record.text))
        }
        IndexCorpusResult.Indexed(vectorIndex.replace(embedded))
    }

    private suspend fun buildRecords(): List<VectorRecord> {
        val surahs = database.quranDao().getAllSurahsOnce().associateBy { it.number }
        val ayahs = database.quranDao().getAllAyahs()
        val result = ArrayList<VectorRecord>(ayahs.size)

        ayahs.forEach { ayah ->
            val surah = surahs[ayah.surahId]
            val reference = "QS. ${surah?.nameLatin ?: ayah.surahId}:${ayah.ayahNumber}"
            val text = listOf(ayah.textArabic, ayah.translationId, ayah.translationEn)
                .filter(String::isNotBlank)
                .joinToString("\n")
            appendChunks(
                result = result,
                sourceType = "quran",
                collectionId = "quran",
                sourceId = "quran-${ayah.surahId}-${ayah.ayahNumber}",
                title = reference,
                reference = reference,
                text = text,
                surahNumber = ayah.surahId,
                ayahNumber = ayah.ayahNumber
            )
        }

        database.hadithDao().getAllHadiths().forEach { hadith ->
            val text = listOf(hadith.textArabic, hadith.translationEn)
                .filter(String::isNotBlank)
                .joinToString("\n")
            appendChunks(
                result = result,
                sourceType = "hadith",
                collectionId = hadith.collectionId,
                sourceId = "hadith-${hadith.id}",
                title = hadith.title.ifBlank { hadith.collectionId },
                reference = hadith.reference.ifBlank { "${hadith.collectionId}:${hadith.hadithNumber}" },
                text = text
            )
        }

        database.knowledgeChunkDao().getAllChunks().forEach { chunk ->
            appendChunks(
                result = result,
                sourceType = chunk.sourceType,
                collectionId = chunk.sourceType,
                sourceId = chunk.sourceId,
                title = chunk.title,
                reference = chunk.sourceId,
                text = chunk.textContent
            )
        }
        return result
    }

    private fun appendChunks(
        result: MutableList<VectorRecord>,
        sourceType: String,
        collectionId: String,
        sourceId: String,
        title: String,
        reference: String,
        text: String,
        surahNumber: Int? = null,
        ayahNumber: Int? = null
    ) {
        chunkText(text).forEachIndexed { index, chunk ->
            result += VectorRecord(
                sourceId = "$sourceId#$index",
                sourceType = sourceType,
                collectionId = collectionId,
                chunkIndex = index,
                text = chunk,
                embedding = FloatArray(0),
                title = title,
                reference = reference,
                identifier = sourceId,
                surahNumber = surahNumber,
                ayahNumber = ayahNumber
            )
        }
    }

    private fun chunkText(text: String): List<String> {
        val words = text.trim().split(Regex("\\s+")).filter(String::isNotBlank)
        if (words.isEmpty()) return emptyList()
        val chunks = mutableListOf<String>()
        var start = 0
        while (start < words.size) {
            val end = (start + CHUNK_SIZE).coerceAtMost(words.size)
            chunks += words.subList(start, end).joinToString(" ")
            if (end == words.size) break
            start = (end - CHUNK_OVERLAP).coerceAtLeast(start + 1)
        }
        return chunks
    }

    private companion object {
        const val CHUNK_SIZE = 512
        const val CHUNK_OVERLAP = 50
    }
}
