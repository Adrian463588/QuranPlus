package com.quranplus.app.features.rag.domain

import java.net.URI

data class HadithReferenceRequest(val collection: String, val number: Int, val suffix: String = "", val archive: Boolean = false) {
    val url: String get() = "https://sunnah.com/$collection:$number$suffix"

    fun matches(citation: RetrievedCitation): Boolean {
        if (citation.evidenceKind != EvidenceKind.HADITH || citation.collection != collection) return false
        if (archive) {
            val target = citation.navigationTarget() as? CitationTarget.Hadith ?: return false
            return !citation.isInternetSourced() && target.collectionId == collection && target.hadithNumber == number
        }
        if (!citation.isInternetSourced() && suffix.isEmpty() &&
            citation.providerId == "local-hadith-bundle" && citation.reference.contains("(Sunnah.com, Kitab ")) {
            return findAll(citation.reference).firstOrNull() == this
        }
        val sourceUrl = citation.canonicalUrl ?: citation.deepLinkTarget ?: return false
        val uri = runCatching { URI(sourceUrl) }.getOrNull() ?: return false
        if (uri.host != "sunnah.com") return false
        if (uri.path == "/$collection:$number$suffix") return true
        if (suffix.isNotEmpty()) return false
        if (Regex("^/$collection:$number[a-z]$").matches(uri.path.orEmpty())) return true
        return false
    }

    companion object {
        private val aliases = linkedMapOf(
            "abudawud" to "(?:(?:abu|abi)[ -]*(?:dawud|daud|daeud|dawood)|abudawud)",
            "bukhari" to "(?:al[ -]*)?bukhari", "muslim" to "muslim",
            "tirmidhi" to "(?:at[ -]*)?(?:tirmidhi|tirmidzi|tirmizi)",
            "nasai" to "(?:an[ -]*)?nasa['’]?i", "ibnmajah" to "ibn[u]?[ -]*majah",
            "ahmad" to "ahmad", "darimi" to "(?:ad[ -]*)?darimi", "malik" to "malik"
        )
        private val pattern = Regex(
            "\\b(${aliases.values.joinToString("|")})\\s*(?:(arsip|gading|tafsirq)\\s*)?(?:no\\.?|nomor|:|#)?\\s*(-?\\d+)([a-z]?)\\b",
            RegexOption.IGNORE_CASE
        )

        fun findAll(text: String): List<HadithReferenceRequest> = pattern.findAll(text).mapNotNull { match ->
            val collection = aliases.entries.firstOrNull {
                Regex(it.value, RegexOption.IGNORE_CASE).matches(match.groupValues[1])
            }?.key ?: return@mapNotNull null
            val number = match.groupValues[3].toIntOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
            val archive = match.groupValues[2].isNotBlank() ||
                Regex("(?i)(?:arsip|gading|tafsirq)\\s+$").containsMatchIn(text.take(match.range.first))
            HadithReferenceRequest(collection, number, match.groupValues[4].lowercase(), archive)
        }.toList()

        fun hasInvalidReference(text: String): Boolean = pattern.findAll(text).any {
            it.groupValues[3].toIntOrNull()?.let { number -> number > 0 } != true
        }
    }
}
