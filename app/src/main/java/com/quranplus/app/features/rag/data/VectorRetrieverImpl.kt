package com.quranplus.app.features.rag.data

import com.quranplus.app.features.rag.domain.RetrievedCitation
import com.quranplus.app.features.rag.domain.VectorIndex
import com.quranplus.app.features.rag.domain.VectorRetriever

class VectorRetrieverImpl(
    private val vectorIndex: VectorIndex
) : VectorRetriever {

    override suspend fun isIndexReady(): Boolean = vectorIndex.coverage().isPopulated

    override suspend fun indexedSourceTypes(): Set<String> =
        vectorIndex.coverage().sourceTypes

    override suspend fun retrieveTopK(
        query: String,
        queryEmbedding: FloatArray,
        k: Int,
        minScore: Float
    ): List<RetrievedCitation> {
        require(k > 0) { "k must be positive" }
        require(query.isNotBlank()) { "query must not be blank" }

        if (!vectorIndex.isReady()) {
            throw VectorIndexUnavailable(
                "sqlite-vec index belum tersedia"
            )
        }
        return vectorIndex.search(queryEmbedding, k)
            .mapNotNull { match ->
                val score = (1f - match.distance).coerceIn(-1f, 1f)
                if (score < minScore) return@mapNotNull null
                val sourceType = match.sourceType.trim().lowercase()
                val collectionId = match.collectionId.trim().takeIf(String::isNotBlank)
                val identifier = match.identifier.trim().ifBlank { match.sourceId }
                val hadithNumber = if (sourceType == "hadith") {
                    parsePositiveInt(identifier) ?: parseHadithReference(match.reference)
                } else {
                    null
                }
                val reference = match.reference.ifBlank {
                    if (sourceType == "hadith" && collectionId != null && hadithNumber != null) {
                        "$collectionId No. $hadithNumber"
                    } else {
                        match.sourceId
                    }
                }
                RetrievedCitation(
                    sourceId = match.sourceId,
                    sourceType = sourceType,
                    title = displayTitle(match.title, sourceType, collectionId, hadithNumber),
                    reference = reference,
                    textSnippet = match.text,
                    score = score,
                    collection = collectionId,
                    identifier = identifier,
                    deepLinkTarget = canonicalTarget(
                        sourceType = sourceType,
                        sourceId = match.sourceId,
                        collectionId = collectionId,
                        hadithNumber = hadithNumber,
                        surahNumber = match.surahNumber,
                        ayahNumber = match.ayahNumber,
                        identifier = identifier
                    ),
                    surahNumber = match.surahNumber,
                    ayahNumber = match.ayahNumber
                )
            }
    }

    private fun canonicalTarget(
        sourceType: String,
        sourceId: String,
        collectionId: String?,
        hadithNumber: Int?,
        surahNumber: Int?,
        ayahNumber: Int?,
        identifier: String
    ): String? = when (sourceType) {
        "quran" -> {
            val surah = surahNumber ?: parseQuranSourceId(sourceId)?.first
            val ayah = ayahNumber ?: parseQuranSourceId(sourceId)?.second
            if (surah != null && ayah != null && surah in 1..114 && ayah > 0) {
                "quran:$surah:$ayah"
            } else {
                parseQuranIdentifier(identifier)?.let {
                    "quran:${it.first}:${it.second}"
                }
            }
        }

        "hadith" -> if (collectionId != null && hadithNumber != null) {
            "hadith:$collectionId:$hadithNumber"
        } else {
            null
        }

        else -> null
    }

    private fun displayTitle(
        title: String,
        sourceType: String,
        collectionId: String?,
        hadithNumber: Int?
    ): String {
        if (sourceType != "hadith" || hadithNumber == null) return title.ifBlank { "Rujukan lokal" }
        val baseTitle = title.ifBlank { collectionId ?: "Hadist" }
        val numberPattern = Regex(
            "(?i)(?:no\\.?|nomor)\\s*${Regex.escape(hadithNumber.toString())}(?:\\b|$)"
        )
        return if (numberPattern.containsMatchIn(baseTitle)) {
            baseTitle
        } else {
            "$baseTitle No. $hadithNumber"
        }
    }

    private fun parsePositiveInt(value: String): Int? =
        value.substringBefore('#').trim().toIntOrNull()?.takeIf { it > 0 }

    private fun parseHadithReference(value: String): Int? =
        Regex("(?i)(?:no\\.?|nomor)\\s*(\\d+)")
            .find(value)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
            ?.takeIf { it > 0 }

    private fun parseQuranIdentifier(value: String): Pair<Int, Int>? {
        val parts = value.substringBefore('#').split(':')
        if (parts.size != 2) return null
        val surah = parts[0].toIntOrNull() ?: return null
        val ayah = parts[1].toIntOrNull() ?: return null
        return surah to ayah
    }

    private fun parseQuranSourceId(value: String): Pair<Int, Int>? {
        val match = Regex("^quran-(\\d+)-(\\d+)(?:#\\d+)?$", RegexOption.IGNORE_CASE)
            .matchEntire(value.trim())
            ?: return null
        val surah = match.groupValues[1].toIntOrNull() ?: return null
        val ayah = match.groupValues[2].toIntOrNull() ?: return null
        return surah to ayah
    }
}
