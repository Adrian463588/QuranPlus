package com.quranplus.app.features.rag.domain

data class CitationValidation(
    val content: String,
    val isValid: Boolean,
    val invalidIds: Set<String> = emptySet(),
    val referencedIds: Set<String> = emptySet()
)

/** Validates the only citation syntax the model is allowed to emit. */
object CitationMarkerValidator {
    private val markerPattern = Regex("\\[\\[cite:([A-Za-z0-9_-]+)]]")

    fun stripMarkers(content: String): String = markerPattern.replace(content, "")

    fun validate(content: String, citations: List<RetrievedCitation>): CitationValidation {
        val allowedIds = citations.indices.mapTo(mutableSetOf()) { "C${it + 1}" }
        val markers = markerPattern.findAll(content).toList()
        val referencedIds = markers.mapTo(mutableSetOf()) { it.groupValues[1] }
        val invalidIds = referencedIds - allowedIds
        val isValid = invalidIds.isEmpty() && referencedIds.isNotEmpty()
        val cleaned = markerPattern.replace(content) { match ->
            if (match.groupValues[1] in allowedIds) match.value else ""
        }
        return CitationValidation(
            content = cleaned,
            isValid = isValid,
            invalidIds = invalidIds,
            referencedIds = referencedIds
        )
    }
}
