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
            )
        }
        return array.toString()
    }

    fun decode(json: String?): List<RetrievedCitation> {
        if (json.isNullOrBlank()) return emptyList()
        val array = JSONArray(json)
        return buildList(array.length()) {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    RetrievedCitation(
                        sourceId = item.getString("source_id"),
                        sourceType = item.getString("source_type"),
                        title = item.getString("title"),
                        reference = item.getString("reference"),
                        textSnippet = item.getString("text_snippet"),
                        score = item.getDouble("score").toFloat(),
                        collection = item.optionalString("collection"),
                        identifier = item.optString("identifier").takeIf(String::isNotBlank)
                            ?: item.getString("source_id"),
                        deepLinkTarget = item.optionalString("deep_link_target"),
                        surahNumber = item.optionalInt("surah_number"),
                        ayahNumber = item.optionalInt("ayah_number")
                    )
                )
            }
        }
    }

    private fun JSONObject.optionalInt(key: String): Int? {
        return if (has(key) && !isNull(key)) getInt(key) else null
    }

    private fun JSONObject.optionalString(key: String): String? {
        return if (has(key) && !isNull(key)) getString(key).takeIf(String::isNotBlank) else null
    }
}
