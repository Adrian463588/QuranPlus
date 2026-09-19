package com.quranplus.app.features.chatbot.data

import com.quranplus.app.features.rag.domain.RetrievedCitation
import org.json.JSONArray
import org.json.JSONObject

/** Room-safe codec for the exact citations attached to an assistant message. */
object CitationJsonCodec {

    fun encode(citations: List<RetrievedCitation>): String {
        val array = JSONArray()
        citations.forEach { citation ->
            array.put(
                JSONObject()
                    .put("source_id", citation.sourceId)
                    .put("source_type", citation.sourceType)
                    .put("title", citation.title)
                    .put("reference", citation.reference)
                    .put("text_snippet", citation.textSnippet)
                    .put("score", citation.score.toDouble())
                    .put("collection", citation.collection)
                    .put("identifier", citation.identifier)
                    .put("deep_link_target", citation.deepLinkTarget)
                    .put("surah_number", citation.surahNumber)
                    .put("ayah_number", citation.ayahNumber)
                    .put("evidence_kind", citation.evidenceKind.name)
                    .put("authority_tier", citation.authorityTier.name)
                    .put("provider_id", citation.providerId)
                    .put("canonical_url", citation.canonicalUrl)
                    .put("retrieved_at", citation.retrievedAt)
            )
        }
        return array.toString()
    }

    fun decode(json: String?): List<RetrievedCitation> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(json)
            buildList(array.length()) {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val sourceId = item.optionalString("source_id") ?: continue
                    val sourceType = item.optionalString("source_type") ?: continue
                    val textSnippet = item.optionalString("text_snippet") ?: continue
                    val title = item.optionalString("title") ?: sourceId
                    val reference = item.optionalString("reference") ?: title
                    val score = item.optDouble("score", 0.0).toFloat().takeIf(Float::isFinite) ?: 0f
                    add(
                        RetrievedCitation(
                            sourceId = sourceId,
                            sourceType = sourceType,
                            title = title,
                            reference = reference,
                            textSnippet = textSnippet,
                            score = score,
                            collection = item.optionalString("collection"),
                            identifier = item.optionalString("identifier") ?: sourceId,
                            deepLinkTarget = item.optionalString("deep_link_target"),
                            surahNumber = item.optionalInt("surah_number"),
                            ayahNumber = item.optionalInt("ayah_number"),
                            evidenceKind = item.optionalString("evidence_kind")
                                ?.let { value -> runCatching { com.quranplus.app.features.rag.domain.EvidenceKind.valueOf(value) }.getOrNull() }
                                ?: com.quranplus.app.features.rag.domain.EvidenceKind.fromSourceType(sourceType),
                            authorityTier = item.optionalString("authority_tier")
                                ?.let { value -> runCatching { com.quranplus.app.features.rag.domain.AuthorityTier.valueOf(value) }.getOrNull() }
                                ?: com.quranplus.app.features.rag.domain.authorityTierFor(sourceType),
                            providerId = item.optionalString("provider_id") ?: "local",
                            canonicalUrl = item.optionalString("canonical_url"),
                            retrievedAt = item.optLong("retrieved_at", 0L).takeIf { it > 0L }
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun JSONObject.optionalInt(key: String): Int? {
        return if (has(key) && !isNull(key)) getInt(key) else null
    }

    private fun JSONObject.optionalString(key: String): String? {
        return if (has(key) && !isNull(key)) getString(key).takeIf(String::isNotBlank) else null
    }
}
