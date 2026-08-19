package com.quranplus.app.core.utils

data class QuranWordRange(
    val wordIndex: Int,
    val start: Int,
    val endExclusive: Int
)

/**
 * Aligns verified source words to the displayed Uthmani string without
 * splitting or assigning a verse-level transliteration heuristically.
 */
fun alignQuranWords(
    displayText: String,
    sourceWords: List<Pair<Int, String>>
): List<QuranWordRange>? {
    if (displayText.isBlank() || sourceWords.isEmpty()) return null
    if (sourceWords.withIndex().any { (index, word) ->
            word.first != index + 1 || word.second.isBlank()
        }
    ) return null

    val ranges = ArrayList<QuranWordRange>(sourceWords.size)
    var cursor = skipKnownBismillahPrefix(displayText)
    sourceWords.forEach { (wordIndex, sourceWord) ->
        val range = findNextWordRange(displayText, cursor, sourceWord) ?: return null
        ranges += QuranWordRange(wordIndex, range.first, range.second)
        cursor = range.second
    }
    if (!isAlignmentGap(displayText, cursor, displayText.length)) return null
    return ranges
}

private fun findNextWordRange(
    displayText: String,
    cursor: Int,
    sourceWord: String
): Pair<Int, Int>? {
    var candidateStart = cursor
    val target = normalizeWord(sourceWord)
    if (target.isEmpty()) return null

    while (candidateStart < displayText.length && isSeparator(displayText, candidateStart)) {
        candidateStart++
    }
    if (!isAlignmentGap(displayText, cursor, candidateStart)) return null

    var candidateEnd = candidateStart
    val candidate = StringBuilder()
    while (candidateEnd < displayText.length && !isSeparator(displayText, candidateEnd)) {
        normalizeWordChar(displayText[candidateEnd])?.let(candidate::append)
        candidateEnd++
    }
    return (candidateStart to candidateEnd).takeIf { candidate.toString() == target }
}

private fun normalizeWord(value: String): String = buildString(value.length) {
    value.forEach { char -> normalizeWordChar(char)?.let(::append) }
}

private fun normalizeWordChar(char: Char): Char? {
    if (char == '\u0640' || char in WaqafParser.WAQAF_MARKER_SYMBOLS || char == WaqafParser.AYAH_END_SYM.single()) {
        return null
    }
    if (Character.getType(char) == Character.NON_SPACING_MARK.toInt() ||
        Character.getType(char) == Character.COMBINING_SPACING_MARK.toInt() ||
        Character.getType(char) == Character.ENCLOSING_MARK.toInt()
    ) return null
    return when (char) {
        'ٱ', 'أ', 'إ', 'آ' -> 'ا'
        'ئ' -> 'ى'
        'ؤ' -> 'و'
        else -> char
    }
}

private fun isAlignmentGap(text: String, start: Int, end: Int): Boolean =
    text.substring(start, end).all(::isAllowedGapChar)

private fun isSeparator(text: String, index: Int): Boolean {
    val char = text[index]
    if (char in WaqafParser.WAQAF_MARKER_SYMBOLS) {
        return isStandaloneWaqafMarker(text, index)
    }
    return char.isWhitespace() ||
        char == '\u200C' ||
        char == '\u06E9' ||
        char == WaqafParser.AYAH_END_SYM.single() ||
        char in '٠'..'٩' ||
        char in '0'..'9'
}

private fun isAllowedGapChar(char: Char): Boolean =
    char.isWhitespace() ||
        char == '\u0640' ||
        char == '\u200C' ||
        char == '\u06E9' ||
        char in WaqafParser.WAQAF_MARKER_SYMBOLS ||
        char == WaqafParser.AYAH_END_SYM.single() ||
        char in '٠'..'٩' ||
        char in '0'..'9'

private fun isStandaloneWaqafMarker(text: String, index: Int): Boolean {
    val previous = adjacentWordCharacter(text, index, direction = -1)
    val next = adjacentWordCharacter(text, index, direction = 1)
    return previous == null || next == null
}

private fun adjacentWordCharacter(text: String, index: Int, direction: Int): Char? {
    var cursor = index + direction
    while (cursor in text.indices) {
        val char = text[cursor]
        if (Character.getType(char) == Character.NON_SPACING_MARK.toInt() ||
            Character.getType(char) == Character.COMBINING_SPACING_MARK.toInt() ||
            Character.getType(char) == Character.ENCLOSING_MARK.toInt() ||
            char == '\u0640'
        ) {
            cursor += direction
            continue
        }
        return char.takeIf { isWordCharacter(it) }
    }
    return null
}

private fun isWordCharacter(char: Char): Boolean =
    !char.isWhitespace() &&
        char != '\u200C' &&
        char != '\u06E9' &&
        char !in WaqafParser.WAQAF_MARKER_SYMBOLS &&
        char != WaqafParser.AYAH_END_SYM.single() &&
        char !in '٠'..'٩' &&
        char !in '0'..'9'

private fun skipKnownBismillahPrefix(text: String): Int {
    val prefix = "بِّسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"
    if (!text.startsWith(prefix)) return 0
    val boundary = prefix.length
    return if (boundary == text.length || text[boundary].isWhitespace()) boundary else 0
}
