package com.quranplus.app.features.quran.presentation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

internal fun highlightSearchMatches(
    text: String,
    query: String,
    highlightColor: Color,
    highlightTextColor: Color
): AnnotatedString {
    val terms = query.trim().split(Regex("\\s+")).filter(String::isNotBlank).distinct()
    val ranges = terms.flatMap { term ->
        generateMatchRanges(text, term)
    }.sortedBy { it.first }
    if (ranges.isEmpty()) return AnnotatedString(text)

    return buildAnnotatedString {
        var cursor = 0
        ranges.forEach { (start, end) ->
            if (start < cursor) return@forEach
            append(text.substring(cursor, start))
            withStyle(SpanStyle(background = highlightColor, color = highlightTextColor)) {
                append(text.substring(start, end))
            }
            cursor = end
        }
        append(text.substring(cursor))
    }
}

private fun generateMatchRanges(text: String, term: String): List<Pair<Int, Int>> {
    val ranges = mutableListOf<Pair<Int, Int>>()
    var start = 0
    while (start < text.length) {
        val match = text.indexOf(term, startIndex = start, ignoreCase = true)
        if (match < 0) break
        ranges += match to (match + term.length)
        start = match + term.length
    }
    return ranges
}
