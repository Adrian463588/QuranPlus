package com.quranplus.app

import com.quranplus.app.features.dzikir.data.DzikirDataCatalog
import com.quranplus.app.features.dzikir.data.DzikirRepositoryImpl
import com.quranplus.app.features.dzikir.data.DzikirTajwidFormatter
import com.quranplus.app.features.dzikir.domain.DzikirCategory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DzikirFeatureTest {

    private val repository = DzikirRepositoryImpl()

    @Test
    fun GIVEN_catalog_WHEN_inspectingItems_THEN_allCategoriesRepresented() {
        val allItems = DzikirDataCatalog.ALL_ITEMS
        assertTrue("Catalog should not be empty", allItems.isNotEmpty())

        DzikirCategory.entries.forEach { category ->
            val count = allItems.count { it.category == category }
            assertTrue("Category ${category.name} should have items in catalog", count > 0)
        }
    }

    @Test
    fun GIVEN_catalog_WHEN_inspectingItems_THEN_allRequiredFieldsArePopulated() {
        DzikirDataCatalog.ALL_ITEMS.forEach { item ->
            assertTrue("Item ${item.id} title cannot be blank", item.title.isNotBlank())
            assertTrue("Item ${item.id} arabicText cannot be blank", item.arabicText.isNotBlank())
            assertTrue("Item ${item.id} transliteration cannot be blank", item.transliteration.isNotBlank())
            assertTrue("Item ${item.id} translationId cannot be blank", item.translationId.isNotBlank())
            assertTrue("Item ${item.id} translationEn cannot be blank", item.translationEn.isNotBlank())
            assertTrue("Item ${item.id} repeatCount must be positive", item.repeatCount > 0)
            assertTrue("Item ${item.id} sourceNote cannot be blank", item.sourceNote.isNotBlank())
        }
    }

    @Test
    fun GIVEN_transliteration_WHEN_checkingIkhfaRules_THEN_usesNasalNgPhonetic() {
        val transliterations = DzikirDataCatalog.ALL_ITEMS.map { it.transliteration }.joinToString(" ")
        assertTrue(
            "Transliteration should contain nasal 'ng' for ikhfa (e.g. ming, yungfiqūn, angta)",
            transliterations.contains("ming") ||
                    transliterations.contains("yungfiqūn") ||
                    transliterations.contains("angta") ||
                    transliterations.contains("ungzila")
        )
    }

    @Test
    fun GIVEN_repository_WHEN_getCategories_THEN_returnsSortedCategories() = runBlocking {
        val categories = repository.getCategories().first()
        assertEquals(DzikirCategory.entries.size, categories.size)
        assertEquals(DzikirCategory.PAGI, categories.first())
    }

    @Test
    fun GIVEN_repository_WHEN_getItemsByCategory_THEN_returnsExpectedCategoryItems() = runBlocking {
        val morningItems = repository.getItemsByCategory(DzikirCategory.PAGI).first()
        assertTrue(morningItems.isNotEmpty())
        assertTrue(morningItems.all { it.category == DzikirCategory.PAGI })

        val hizibBahrItems = repository.getItemsByCategory(DzikirCategory.HIZIB_BAHR).first()
        assertTrue(hizibBahrItems.isNotEmpty())
        assertTrue(hizibBahrItems.all { it.category == DzikirCategory.HIZIB_BAHR })
    }

    @Test
    fun GIVEN_searchQuery_WHEN_searchDzikir_THEN_matchesRelevantItems() = runBlocking {
        val istighfarResults = repository.searchDzikir("Istighfar").first()
        assertTrue("Searching 'Istighfar' should return matching items", istighfarResults.isNotEmpty())
        assertTrue(istighfarResults.any { it.title.contains("Istighfar", ignoreCase = true) })

        val bahrResults = repository.searchDzikir("Bahr").first()
        assertTrue("Searching 'Bahr' should return matching items", bahrResults.isNotEmpty())
    }

    @Test
    fun GIVEN_tajwidFormattedArabic_WHEN_enableTajwid_THEN_generatesStyledAnnotatedString() {
        val taggedArabic = "يَا عَلِيُّ [f[أَنتَ] رَبِّي"
        val annotated = DzikirTajwidFormatter.formatArabic(
            arabicText = taggedArabic,
            tajwidTags = taggedArabic,
            enableTajwid = true
        )
        assertNotNull(annotated)
        assertTrue(annotated.text.isNotBlank())
        assertEquals("يَا عَلِيُّ أَنتَ رَبِّي", annotated.text)

        val disabled = DzikirTajwidFormatter.formatArabic(
            arabicText = taggedArabic,
            tajwidTags = taggedArabic,
            enableTajwid = false
        )
        assertNotNull(disabled)
        assertFalse(disabled.text.contains("["))
        assertFalse(disabled.text.contains("]"))
    }

    @Test
    fun GIVEN_hizibNashr_WHEN_inspected_THEN_containsAll6SectionsAndCompleteTexts() = runBlocking {
        val items = repository.getItemsByCategory(DzikirCategory.HIZIB_NASHR).first()
        assertEquals("Hizib Nashr must have 6 comprehensive sections", 6, items.size)
        assertTrue("Must contain Muqatha'ah section", items.any { it.title.contains("Muqatha'ah", ignoreCase = true) })
        assertTrue("Must contain Ha Mim section", items.any { it.title.contains("Ha Mim", ignoreCase = true) })
        assertTrue("Must contain Gharatullah", items.any { it.title.contains("Ghāratullāh") || it.title.contains("Gharatullah", ignoreCase = true) })
        // Verify tajwid tags are present
        assertTrue("Must contain mad tags in Hizib Nashr", items.any { it.tajwidTags?.contains("[o[") == true || it.tajwidTags?.contains("[n[") == true || it.tajwidTags?.contains("[p[") == true })
        assertTrue("Must contain ikhfa tags in Hizib Nashr", items.any { it.tajwidTags?.contains("[f[") == true || it.tajwidTags?.contains("[c[") == true })
    }

    @Test
    fun GIVEN_hizibBahr_WHEN_inspected_THEN_containsAll7SectionsAndCompleteTexts() = runBlocking {
        val items = repository.getItemsByCategory(DzikirCategory.HIZIB_BAHR).first()
        assertEquals("Hizib Bahr must have 7 comprehensive sections", 7, items.size)
        assertTrue("Must contain Bismillahi Babuna", items.any { it.title.contains("Bābunā") || it.transliteration.contains("Bismillāhi bābunā") })
        assertTrue("Must contain Syahatil Wujuh", items.any { it.title.contains("Syāhatil Wujūh") || it.transliteration.contains("Syāhatil wujūh") })
        assertTrue("Must contain Sitrul Arsy", items.any { it.title.contains("Sitrul 'Arsy") || it.transliteration.contains("Sitrul-'arsyi") })
    }

    @Test
    fun GIVEN_hizibNawawi_WHEN_inspected_THEN_containsAll6SectionsAndCompleteTexts() = runBlocking {
        val items = repository.getItemsByCategory(DzikirCategory.HIZIB_NAWAWI).first()
        assertEquals("Hizib Nawawi must have 6 comprehensive sections", 6, items.size)
        assertTrue("Must contain Alfa Alfi Hauqalah", items.any { it.title.contains("Hauqalah") || it.transliteration.contains("Alfa alfi") })
        assertTrue("Must contain Hasbiyallah 7x", items.any { it.title.contains("Hasbiyallah", ignoreCase = true) || it.transliteration.contains("asbiyall", ignoreCase = true) })
    }

    @Test
    fun GIVEN_shalatFardhu_WHEN_inspected_THEN_containsAll12SectionsAndCompleteTexts() = runBlocking {
        val items = repository.getItemsByCategory(DzikirCategory.SHALAT).first()
        assertEquals("Setelah Shalat must have 12 standard sections", 12, items.size)
        assertTrue("Must contain Istighfar", items.any { it.title.contains("Istighfar") })
        assertTrue("Must contain Doa Lindungan Neraka 7x", items.any { it.title.contains("Neraka") || it.transliteration.contains("ajirnā minan-nār") })
        assertTrue("Must contain Doa Keselamatan", items.any { it.title.contains("Keselamatan") || it.transliteration.contains("Antas-salām") })
        assertTrue("Must contain Doa Pujian Kemutlakan Takdir", items.any { it.title.contains("Kemutlakan") || it.transliteration.contains("māni‘a") })
        assertTrue("Must contain Ayat Kursi", items.any { it.title.contains("Kursi") })
        assertTrue("Must contain Tasbih Tahmid Takbir", items.any { it.title.contains("Tasbih") })
        assertTrue("Must contain Surat Al-Fatihah", items.any { it.title.contains("Al-Fatihah") })
        assertTrue("Must contain Doa Birrul Walidain", items.any { it.title.contains("Birrul Walidain") })
    }

    @Test
    fun GIVEN_hizibNashrHaddad_WHEN_inspected_THEN_containsAll5SectionsAndCompleteTexts() = runBlocking {
        val items = repository.getItemsByCategory(DzikirCategory.HIZIB_NASHR_HADDAD).first()
        assertEquals("Hizib Nashr Al-Haddad must have 5 comprehensive sections", 5, items.size)
        assertTrue("Must contain Surat Al-Fath opener", items.any { it.title.contains("Al-Fath") || it.transliteration.contains("fataḥnā") })
    }

    @Test
    fun GIVEN_alMatsurat_WHEN_inspected_THEN_containsAll28SectionsAndCompleteTexts() = runBlocking {
        val items = repository.getItemsByCategory(DzikirCategory.AL_MATSURAT).first()
        assertEquals("Al-Matsurat Sughra must have 28 comprehensive sections", 28, items.size)
        assertTrue("Must contain Doa 'Afiyah", items.any { it.title.contains("Afiyah") || it.transliteration.contains("badanī") })
        assertTrue("Must contain Hasbiyallah 7x", items.any { it.title.contains("Hasbiyallāh") || it.transliteration.contains("Ḥasbiyallāhu") })
        assertTrue("Must contain Ayat Mulk", items.any { it.title.contains("Mulk") || it.transliteration.contains("mālikal-mulki") })
    }

    @Test
    fun GIVEN_tajwidParser_WHEN_parsingMadTabii_THEN_madColorIsApplied() {
        val taggedArabic = "[n[قَالَ]]"
        val parsed = com.quranplus.app.core.utils.TajwidParser.parseBracketTags(taggedArabic)
        assertEquals("قَالَ", parsed.text)
        assertEquals(1, parsed.spans.size)
        val span = parsed.spans.first()
        assertEquals(com.quranplus.app.core.utils.TajwidParser.TajwidType.MAD_TABII, span.type)
        assertNotNull("Mad Tabi'i rule must have non-null color", span.type.color)
    }
}

