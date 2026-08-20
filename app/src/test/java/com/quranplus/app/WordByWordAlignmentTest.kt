package com.quranplus.app

import androidx.compose.ui.text.AnnotatedString
import com.quranplus.app.core.utils.WaqafParser
import com.quranplus.app.features.quran.domain.WordByWord
import com.quranplus.app.features.quran.presentation.buildWordRenderSlices
import com.quranplus.app.features.quran.presentation.extractAyahEndMarker
import com.quranplus.app.features.quran.presentation.wordTranslations
import com.quranplus.app.features.settings.data.TranslationMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class WordByWordAlignmentTest {

    @Test
    fun GIVEN_sequentialSourceWords_WHEN_aligningAyah_THEN_preservesWaqafAndEndMarkerOnce() {
        val words = listOf(
            word(1, "ذَٰلِكَ"),
            word(2, "ٱلْكِتَٰبُ"),
            word(3, "لَا")
        )
        val text = AnnotatedString(
            "ذَٰلِكَ ٱلْكِتَٰبُ ${WaqafParser.WAQAF_MUANAQAH_SYM} لَا" +
                WaqafParser.formatAyahEndMarker(2)
        )

        val slices = buildWordRenderSlices(words, text)

        assertNotNull(slices)
        assertEquals(3, slices!!.size)
        assertEquals(0, slices.joinToString("") { it.text.text }.count { it == '۝' })
        assertEquals(WaqafParser.WAQAF_MUANAQAH_SYM, slices[1].text.text.substringAfter("ٱلْكِتَٰبُ").trim())
        assertEquals("۝٢", extractAyahEndMarker(text)?.text?.trim())
    }

    @Test
    fun GIVEN_mismatchedArabicSource_WHEN_aligningAyah_THEN_returnsUnavailable() {
        val slices = buildWordRenderSlices(
            words = listOf(word(1, "نص مختلف")),
            annotatedAyah = AnnotatedString("النص")
        )

        assertNull(slices)
    }

    @Test
    fun GIVEN_verifiedUthmaniVariant_WHEN_aligningAyah_THEN_keepsWordAvailable() {
        val slices = buildWordRenderSlices(
            words = listOf(word(1, "هُدًى")),
            annotatedAyah = AnnotatedString("هُدًۭى ۝١")
        )

        assertNotNull(slices)
        assertEquals("هُدًۭى", slices!!.single().text.text)
        assertEquals("۝١", extractAyahEndMarker(AnnotatedString("هُدًۭى ۝١"))?.text?.trim())
    }

    @Test
    fun GIVEN_verifiedCompositeSourceWord_WHEN_aligningAyah_THEN_keepsItsFullArabicRange() {
        val slices = buildWordRenderSlices(
            words = listOf(word(1, "إِلْ يَاسِينَ")),
            annotatedAyah = AnnotatedString("إِلْ يَاسِينَ ۝١")
        )

        assertNotNull(slices)
        assertEquals("إِلْ يَاسِينَ", slices!!.single().text.text)
    }

    @Test
    fun GIVEN_indonesianWordSource_WHEN_selectingIndonesian_THEN_doesNotFallbackToEnglish() {
        val word = word(1, "بِسْمِ").copy(
            translationEn = "In (the) name",
            translationId = "dengan nama"
        )

        assertEquals(
            listOf("Indonesia" to "dengan nama"),
            wordTranslations(word, TranslationMode.INDONESIAN)
        )
        assertEquals(
            listOf("Indonesia" to "dengan nama", "English (source)" to "In (the) name"),
            wordTranslations(word, TranslationMode.BOTH)
        )
    }

    private fun word(index: Int, text: String) = WordByWord(
        id = index.toLong(),
        surahNumber = 2,
        ayahNumber = 2,
        wordIndex = index,
        textArabic = text,
        transliteration = null,
        translationEn = "source",
        translationId = null
    )
}
