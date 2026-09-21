package com.quranplus.app.features.rag.domain

data class CitationValidation(
    val content: String,
    val isValid: Boolean,
    val invalidIds: Set<String> = emptySet(),
    val referencedIds: Set<String> = emptySet()
)

/** Validates the citation syntax the model is allowed to emit. */
object CitationMarkerValidator {
    private val markerPattern = Regex("\\[+\\s*cite:\\s*([A-Za-z0-9_-]+)\\s*]+|\\[+\\s*([Cc]\\d+)\\s*]+|\\[+\\s*]+|\\(\\s*C\\d+\\s*\\)")

    fun stripMarkers(content: String): String = markerPattern.replace(content, "")

    fun validate(content: String, citations: List<RetrievedCitation>): CitationValidation {
        val allowedIds = citations.indices.mapTo(mutableSetOf()) { "C${it + 1}" }
        val markers = markerPattern.findAll(content).toList()
        val referencedIds = markers.mapTo(mutableSetOf()) { match ->
            match.groupValues[1].ifEmpty {
                match.groupValues[2].ifEmpty {
                    match.groupValues[3]
                }
            }
        }
        val invalidIds = referencedIds - allowedIds
        val isValid = invalidIds.isEmpty() && referencedIds.isNotEmpty()
        val cleaned = markerPattern.replace(content) { match ->
            val id = match.groupValues[1].ifEmpty {
                match.groupValues[2].ifEmpty {
                    match.groupValues[3]
                }
            }
            if (id in allowedIds) "[[cite:$id]]" else ""
        }
        return CitationValidation(
            content = cleaned,
            isValid = isValid,
            invalidIds = invalidIds,
            referencedIds = referencedIds
        )
    }
}
