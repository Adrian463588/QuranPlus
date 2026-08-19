package com.quranplus.app.features.quran.data

import com.quranplus.app.features.quran.domain.QuranSearchField
import com.quranplus.app.features.quran.domain.QuranSearchFilter
import com.quranplus.app.features.quran.domain.QuranSearchMode

internal fun buildFtsMatchExpression(
    query: String,
    filter: QuranSearchFilter
): String {
    val cleanQuery = query.trim()
    if (cleanQuery.isBlank()) return ""

    val searchableQuery = when (filter.field) {
        QuranSearchField.ALL,
        QuranSearchField.ARABIC -> normalizeArabicSearchQuery(cleanQuery)
        else -> cleanQuery
    }.trim()
    if (searchableQuery.isBlank()) return ""

    val expression = when (filter.mode) {
        QuranSearchMode.ALL_WORDS -> searchableQuery
            .split(Regex("\\s+"))
            .filter(String::isNotBlank)
            .joinToString(" AND ") { token -> "\"${escapeFtsPhrase(token)}\"*" }

        QuranSearchMode.EXACT_PHRASE ->
            "\"${escapeFtsPhrase(searchableQuery)}\""
    }

    return "${ftsColumns(filter.field)} : ($expression)"
}

internal fun normalizeArabicSearchQuery(query: String): String = buildString(query.length) {
    query.forEach { character ->
        when (character.code) {
            1600, in 1611..1648, in 1750..1773 -> Unit
            1649 -> append('\u0627')
            else -> append(character)
        }
    }
}

private fun ftsColumns(field: QuranSearchField): String = when (field) {
    QuranSearchField.ALL ->
        "{translation_id translation_en transliteration text_arabic text_arabic_normalized}"

    QuranSearchField.ARABIC -> "{text_arabic text_arabic_normalized}"
    QuranSearchField.INDONESIAN -> "translation_id"
    QuranSearchField.ENGLISH -> "translation_en"
    QuranSearchField.TRANSLITERATION -> "transliteration"
}

private fun escapeFtsPhrase(value: String): String = value.replace("\"", "\"\"")
