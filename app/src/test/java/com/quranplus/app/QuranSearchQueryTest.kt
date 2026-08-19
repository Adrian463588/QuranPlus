package com.quranplus.app

import com.quranplus.app.features.quran.data.buildFtsMatchExpression
import com.quranplus.app.features.quran.domain.QuranSearchField
import com.quranplus.app.features.quran.domain.QuranSearchFilter
import com.quranplus.app.features.quran.domain.QuranSearchMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranSearchQueryTest {

    @Test
    fun GIVEN_indonesianAllWords_WHEN_buildingFtsExpression_THEN_onlyIndonesianColumnIsUsed() {
        val expression = buildFtsMatchExpression(
            query = "jalan lurus",
            filter = QuranSearchFilter(field = QuranSearchField.INDONESIAN)
        )

        assertEquals(
            "translation_id : (\"jalan\"* AND \"lurus\"*)",
            expression
        )
    }

    @Test
    fun GIVEN_englishPhrase_WHEN_buildingFtsExpression_THEN_phraseIsKeptTogether() {
        val expression = buildFtsMatchExpression(
            query = "the straight path",
            filter = QuranSearchFilter(
                field = QuranSearchField.ENGLISH,
                mode = QuranSearchMode.EXACT_PHRASE
            )
        )

        assertEquals("translation_en : (\"the straight path\")", expression)
    }

    @Test
    fun GIVEN_arabicDiacritics_WHEN_buildingFtsExpression_THEN_queryUsesNormalizedArabic() {
        val expression = buildFtsMatchExpression(
            query = "اللَّهِ",
            filter = QuranSearchFilter(field = QuranSearchField.ARABIC)
        )

        assertEquals(
            "{text_arabic text_arabic_normalized} : (\"الله\"*)",
            expression
        )
    }

    @Test
    fun GIVEN_allFields_WHEN_buildingFtsExpression_THEN_allIndexedSourcesAreScoped() {
        val expression = buildFtsMatchExpression(
            query = "rahman",
            filter = QuranSearchFilter()
        )

        assertTrue(expression.startsWith("{translation_id translation_en transliteration"))
        assertTrue(expression.contains("\"rahman\"*"))
    }
}
