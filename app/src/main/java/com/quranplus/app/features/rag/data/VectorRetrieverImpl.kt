package com.quranplus.app.features.rag.data

import com.quranplus.app.core.database.dao.HadithDao
import com.quranplus.app.core.database.dao.KnowledgeChunkDao
import com.quranplus.app.core.database.dao.QuranDao
import com.quranplus.app.core.utils.VecMath
import com.quranplus.app.core.utils.VecMath.toFloatArray
import com.quranplus.app.features.rag.domain.RetrievedCitation
import com.quranplus.app.features.rag.domain.VectorRetriever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VectorRetrieverImpl(
    private val chunkDao: KnowledgeChunkDao,
    private val quranDao: QuranDao,
    private val hadithDao: HadithDao
) : VectorRetriever {

    override suspend fun retrieveTopK(
        query: String,
        queryEmbedding: FloatArray,
        k: Int
    ): List<RetrievedCitation> = withContext(Dispatchers.Default) {
        val citations = mutableListOf<RetrievedCitation>()

        // 1. Vector similarity search over embedded knowledge chunks
        val chunks = chunkDao.getAllChunks()
        if (chunks.isNotEmpty()) {
            val scored = chunks.mapNotNull { chunk ->
                val embeddingBlob = chunk.embedding ?: return@mapNotNull null
                val chunkVector = embeddingBlob.toFloatArray()
                val score = VecMath.cosineSimilarity(queryEmbedding, chunkVector)
                RetrievedCitation(
                    sourceId = chunk.sourceId,
                    sourceType = chunk.sourceType,
                    title = chunk.title,
                    reference = chunk.sourceId,
                    textSnippet = chunk.textContent,
                    score = score
                )
            }.sortedByDescending { it.score }.take(k)

            citations.addAll(scored)
        }

        // 2. Fallback / Hybrid Search: Quran FTS5 & Hadith search
        if (citations.size < k) {
            val cleanQuery = query.replace("[^a-zA-Z0-9\\s]".toRegex(), "").trim()
            if (cleanQuery.isNotBlank()) {
                val ftsAyahs = runCatching { quranDao.searchAyahsFts(cleanQuery, limit = k) }
                    .getOrElse { quranDao.searchAyahsLike(cleanQuery, limit = k) }

                for (ayah in ftsAyahs) {
                    if (citations.none { it.sourceType == "quran" && it.ayahNumber == ayah.ayahNumber && it.surahNumber == ayah.surahId }) {
                        citations.add(
                            RetrievedCitation(
                                sourceId = "QS. ${ayah.surahId}:${ayah.ayahNumber}",
                                sourceType = "quran",
                                title = "QS. Surah ${ayah.surahId} Ayat ${ayah.ayahNumber}",
                                reference = "QS. ${ayah.surahId}:${ayah.ayahNumber}",
                                textSnippet = "${ayah.textArabic}\n${ayah.translationId}",
                                score = 0.95f,
                                surahNumber = ayah.surahId,
                                ayahNumber = ayah.ayahNumber
                            )
                        )
                    }
                }

                // Hadith search
                val hadiths = hadithDao.getAllHadiths(50)
                val matchingHadiths = hadiths.filter {
                    it.title.contains(cleanQuery, ignoreCase = true) ||
                    it.translationId.contains(cleanQuery, ignoreCase = true) ||
                    it.textArabic.contains(cleanQuery)
                }.take(k)

                for (h in matchingHadiths) {
                    if (citations.none { it.sourceType == "hadith" && it.title == h.title }) {
                        citations.add(
                            RetrievedCitation(
                                sourceId = "${h.collectionId}_${h.hadithNumber}",
                                sourceType = "hadith",
                                title = "${h.collectionId.uppercase()} #${h.hadithNumber} - ${h.title}",
                                reference = h.reference.ifBlank { "HR. ${h.collectionId.uppercase()} No. ${h.hadithNumber}" },
                                textSnippet = "${h.textArabic}\n${h.translationId}",
                                score = 0.90f
                            )
                        )
                    }
                }
            }
        }

        citations.sortedByDescending { it.score }.take(k)
    }
}
