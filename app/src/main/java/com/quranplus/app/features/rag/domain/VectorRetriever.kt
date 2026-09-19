package com.quranplus.app.features.rag.domain

data class RetrievedCitation(
    val sourceId: String,
    val sourceType: String, // "quran", "hadith", "tahsin"
    val title: String,
    val reference: String,
    val textSnippet: String,
    val score: Float,
    val collection: String? = null,
    val identifier: String = sourceId,
    val deepLinkTarget: String? = null,
    val surahNumber: Int? = null,
    val ayahNumber: Int? = null,
    val evidenceKind: EvidenceKind = EvidenceKind.fromSourceType(sourceType),
    val authorityTier: AuthorityTier = authorityTierFor(sourceType),
    val providerId: String = "local",
    val canonicalUrl: String? = null,
    val retrievedAt: Long? = null
)

sealed interface CitationTarget {
    data class Quran(
        val surahNumber: Int,
        val ayahNumber: Int
    ) : CitationTarget

    data class Hadith(
        val collectionId: String,
        val hadithNumber: Int
    ) : CitationTarget

    data class Web(
        val url: String
    ) : CitationTarget
}

/** Resolves the canonical in-app source target, including citations saved by older builds. */
fun RetrievedCitation.navigationTarget(): CitationTarget? {
    parseCanonicalTarget(deepLinkTarget)?.let { return it }

    return when (sourceType.trim().lowercase()) {
        "quran" -> {
            validQuranTarget(surahNumber, ayahNumber)
                ?: parseQuranIdentifier(identifier)
                ?: parseQuranSourceId(sourceId)
        }

        "hadith" -> {
            val collectionId = collection?.trim()?.takeIf(String::isNotBlank)
            val hadithNumber = parsePositiveInt(identifier)
                ?: parseHadithReference(reference)
            if (collectionId != null && hadithNumber != null) {
                CitationTarget.Hadith(collectionId, hadithNumber)
            } else {
                parseHadithSourceId(sourceId)
            }
        }

        "internet", "web" -> parseWebTarget(deepLinkTarget ?: canonicalUrl)

        else -> null
    }
}

private fun parseCanonicalTarget(value: String?): CitationTarget? {
    parseWebTarget(value)?.let { return it }
    val parts = value?.trim()?.split(':') ?: return null
    if (parts.size < 3) return null

    return when (parts[0].lowercase()) {
        "quran" -> validQuranTarget(
            parts[1].toIntOrNull(),
            parts[2].toIntOrNull()
        )

        "hadith" -> {
            val collectionId = parts[1].trim().takeIf(String::isNotBlank)
            val hadithNumber = parts[2].toIntOrNull()?.takeIf { it > 0 }
            if (collectionId != null && hadithNumber != null) {
                CitationTarget.Hadith(collectionId, hadithNumber)
            } else {
                null
            }
        }

        else -> null
    }
}

private fun parseWebTarget(value: String?): CitationTarget.Web? {
    return CitationTargetValidator.validateHttpsUrl(value)?.let { url -> CitationTarget.Web(url) }
}

private fun validQuranTarget(surahNumber: Int?, ayahNumber: Int?): CitationTarget.Quran? {
    if (surahNumber == null || surahNumber !in 1..114 || ayahNumber == null || ayahNumber <= 0) return null
    return CitationTarget.Quran(surahNumber, ayahNumber)
}

private fun parseQuranIdentifier(value: String): CitationTarget.Quran? {
    val parts = value.trim().split(':')
    if (parts.size != 2) return null
    return validQuranTarget(parts[0].toIntOrNull(), parts[1].toIntOrNull())
}

private fun parseQuranSourceId(value: String): CitationTarget.Quran? {
    val match = Regex("^quran-(\\d+)-(\\d+)(?:#\\d+)?$", RegexOption.IGNORE_CASE)
        .matchEntire(value.trim())
        ?: return null
    return validQuranTarget(match.groupValues[1].toIntOrNull(), match.groupValues[2].toIntOrNull())
}

private fun parsePositiveInt(value: String): Int? =
    value.trim().toIntOrNull()?.takeIf { it > 0 }

private fun parseHadithReference(value: String): Int? =
    Regex("(?i)(?:no\\.?|nomor)\\s*(\\d+)")
        .find(value)
        ?.groupValues
        ?.getOrNull(1)
        ?.toIntOrNull()
        ?.takeIf { it > 0 }

private fun parseHadithSourceId(value: String): CitationTarget.Hadith? {
    val match = Regex("^hadith-(.+)-(\\d+)(?:#\\d+)?$", RegexOption.IGNORE_CASE)
        .matchEntire(value.trim())
        ?: return null
    val collectionId = match.groupValues[1].trim().takeIf(String::isNotBlank) ?: return null
    val hadithNumber = match.groupValues[2].toIntOrNull()?.takeIf { it > 0 } ?: return null
    return CitationTarget.Hadith(collectionId, hadithNumber)
}

interface VectorRetriever {
    suspend fun isIndexReady(): Boolean = false

    suspend fun indexedSourceTypes(): Set<String> = emptySet()

    suspend fun retrieveTopK(
        query: String,
        queryEmbedding: FloatArray,
        k: Int = 5,
        minScore: Float = 0.35f
    ): List<RetrievedCitation>
}
