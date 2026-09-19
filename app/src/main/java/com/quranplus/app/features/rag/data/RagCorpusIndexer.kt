package com.quranplus.app.features.rag.data

import android.util.Log
import com.quranplus.app.core.database.QuranDatabase
import com.quranplus.app.features.rag.domain.IndexCorpusResult
import com.quranplus.app.features.rag.domain.RagIndexMetadata
import com.quranplus.app.features.rag.domain.RagRuntimeCoordinator
import com.quranplus.app.features.rag.domain.VectorIndex
import com.quranplus.app.features.rag.domain.VectorIndexCoverage
import com.quranplus.app.features.rag.domain.VectorRecord
import com.quranplus.app.features.hadith.data.HadithBundleManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.yield
import kotlinx.coroutines.withContext
import java.security.MessageDigest

/** Builds one index from the installed Quran, imported Hadith, and RAG documents. */
class RagCorpusIndexer(
    private val database: QuranDatabase,
    private val embeddingService: EmbeddingService,
    private val vectorIndex: VectorIndex,
    private val runtimeCoordinator: RagRuntimeCoordinator
) {
    suspend fun index(): IndexCorpusResult = withContext(Dispatchers.Default) {
        if (!embeddingService.isReady()) return@withContext IndexCorpusResult.Blocked("EMBEDDER_UNAVAILABLE")
        if (!vectorIndex.isReady()) return@withContext IndexCorpusResult.Blocked("INDEX_UNAVAILABLE")

        val records = buildRecords()
        if (records.isEmpty()) return@withContext IndexCorpusResult.Blocked("CORPUS_UNAVAILABLE")
        val contract = embeddingService.embeddingContract()
        val metadata = buildMetadata(records, contract)
        val existingCoverage = vectorIndex.coverage()
        val existingMetadata = vectorIndex.metadata()
        if (existingMetadata?.copy(updatedAt = metadata.updatedAt) == metadata &&
            existingCoverage.recordCount == records.size &&
            hasMinimumCorpusCoverage(existingCoverage)
        ) {
            Log.i(TAG, "RAG index is current; skip rebuild (${existingCoverage.recordCount} records)")
            return@withContext IndexCorpusResult.Indexed(existingCoverage.recordCount)
        }

        Log.i(TAG, "Building RAG index for ${records.size} records")

        val indexedCount = runtimeCoordinator.withExclusiveIndex {
            val embedded = ArrayList<VectorRecord>(records.size)
            records.forEachIndexed { index, record ->
                embedded += record.copy(embedding = embeddingService.embed(record.text))
                if (index % YIELD_INTERVAL == 0) yield()
            }
            vectorIndex.replace(embedded, metadata)
        }
        Log.i(TAG, "RAG index ready ($indexedCount records)")
        IndexCorpusResult.Indexed(indexedCount)
    }

    private suspend fun hasMinimumCorpusCoverage(
        coverage: VectorIndexCoverage
    ): Boolean {
        if (!coverage.isPopulated || "quran" !in coverage.sourceTypes) return false

        val quranCount = database.quranDao().countAyahs()
        if ((coverage.recordCountsBySourceType["quran"] ?: 0) < quranCount) return false

        val hadithCounts = verifiedHadithCollectionCounts()
        val hadithCount = if (HadithBundleManifest.VERIFIED.isVerifiedCorpus(hadithCounts)) {
            hadithCounts.values.sum()
        } else {
            0
        }
        if (hadithCount > 0 &&
            (coverage.recordCountsBySourceType["hadith"] ?: 0) < hadithCount
        ) {
            return false
        }

        val documentSourceCounts = database.knowledgeChunkDao()
            .getAllChunks()
            .groupingBy { it.sourceType }
            .eachCount()
        return documentSourceCounts.all { (sourceType, minimumCount) ->
            sourceType in coverage.sourceTypes &&
                (coverage.recordCountsBySourceType[sourceType] ?: 0) >= minimumCount
        }
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

        val verifiedHadith = if (
            HadithBundleManifest.VERIFIED.isVerifiedCorpus(verifiedHadithCollectionCounts())
        ) {
            database.hadithDao().getAllHadiths()
                .filter(HadithBundleManifest.VERIFIED::isVerifiedRecord)
        } else {
            emptyList()
        }
        verifiedHadith.forEach { hadith ->
            val text = listOf(hadith.textArabic, hadith.translationId, hadith.translationEn)
                .filter(String::isNotBlank)
                .joinToString("\n")
            appendChunks(
                result = result,
                sourceType = "hadith",
                collectionId = hadith.collectionId,
                sourceId = "hadith-${hadith.id}",
                title = hadith.title.ifBlank { hadith.collectionId },
                reference = hadith.reference.ifBlank { "${hadith.collectionId}:${hadith.hadithNumber}" },
                text = text,
                identifier = hadith.hadithNumber.toString()
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
        return result.distinctBy { "${it.sourceType}|${it.sourceId}|${it.chunkIndex}" }
    }

    private suspend fun verifiedHadithCollectionCounts(): Map<String, Int> =
        database.hadithDao().getVerifiedBundleCollectionCounts(
            sourceRevision = HadithBundleManifest.VERIFIED.revision,
            sourceSha256 = HadithBundleManifest.VERIFIED.archiveSha256,
            licenseStatus = "licensed"
        ).associate { it.collectionId to it.recordCount }

    private fun buildMetadata(
        records: List<VectorRecord>,
        contract: EmbeddingContract
    ): RagIndexMetadata {
        val corpusFingerprint = fingerprint(records) { record ->
            listOf(
                record.sourceType,
                record.collectionId,
                record.sourceId,
                record.chunkIndex.toString(),
                record.text
            ).joinToString("\u001f")
        }
        val fingerprint = sha256(
            listOf(
                corpusFingerprint,
                contract.modelId,
                contract.modelRevision,
                contract.tokenizerSha256,
                contract.dimension.toString(),
                contract.normalized.toString(),
                contract.pooling,
                contract.maxSequenceLength.toString(),
                MAX_CONTENT_TOKENS.toString(),
                CHUNK_OVERLAP_TOKENS.toString()
            ).joinToString("\u001f")
        )
        return RagIndexMetadata(
            fingerprint = fingerprint,
            modelId = contract.modelId,
            modelRevision = contract.modelRevision,
            tokenizerSha256 = contract.tokenizerSha256,
            embeddingDimension = contract.dimension,
            normalized = contract.normalized,
            pooling = contract.pooling,
            maxSequenceLength = contract.maxSequenceLength,
            chunkTokenCount = MAX_CONTENT_TOKENS,
            chunkOverlapTokens = CHUNK_OVERLAP_TOKENS,
            corpusRecordCount = records.size,
            corpusFingerprint = corpusFingerprint,
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun fingerprint(records: List<VectorRecord>, value: (VectorRecord) -> String): String =
        sha256(records.joinToString("\u001e", transform = value))

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun appendChunks(
        result: MutableList<VectorRecord>,
        sourceType: String,
        collectionId: String,
        sourceId: String,
        title: String,
        reference: String,
        text: String,
        identifier: String = sourceId,
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
                identifier = identifier,
                surahNumber = surahNumber,
                ayahNumber = ayahNumber
            )
        }
    }

    private fun chunkText(text: String): List<String> {
        val words = text.trim().split(Regex("\\s+")).filter(String::isNotBlank)
        if (words.isEmpty()) return emptyList()
        val tokenCounts = words.map { embeddingService.countContentTokens(it).coerceAtLeast(1) }
        val chunks = mutableListOf<String>()
        var start = 0
        while (start < words.size) {
            var end = start
            var tokenCount = 0
            while (end < words.size &&
                (tokenCount == 0 || tokenCount + tokenCounts[end] <= MAX_CONTENT_TOKENS)
            ) {
                tokenCount += tokenCounts[end]
                end++
            }
            chunks += words.subList(start, end).joinToString(" ")
            if (end == words.size) break

            var overlapStart = end
            var overlapTokens = 0
            while (overlapStart > start && overlapTokens < CHUNK_OVERLAP_TOKENS) {
                overlapStart--
                overlapTokens += tokenCounts[overlapStart]
            }
            start = overlapStart.coerceAtLeast(start + 1)
        }
        return chunks
    }

    private companion object {
        const val TAG = "RagCorpusIndexer"
        // Reserve two positions for [CLS]/[SEP] in the 512-token MiniLM window.
        const val MAX_CONTENT_TOKENS = 510
        const val CHUNK_OVERLAP_TOKENS = 50
        const val YIELD_INTERVAL = 32
    }
}
