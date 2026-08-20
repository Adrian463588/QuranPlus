package com.quranplus.app

import com.quranplus.app.features.hadith.domain.HadithCollection
import com.quranplus.app.features.hadith.domain.HadithCollectionSection
import com.quranplus.app.features.hadith.domain.sectionedHadithCollections
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HadithCollectionSectionTest {

    @Test
    fun GIVEN_canonical_collections_WHEN_grouping_THEN_kutubus_sittah_is_first_and_ordered() {
        val collections = listOf(
            collection("riyad_assalihin", "Riyad as-Salihin"),
            collection("tirmidhi", "Jami' al-Tirmidhi"),
            collection("bukhari", "Sahih al-Bukhari"),
            collection("abudawud", "Sunan Abu Dawud"),
            collection("muslim", "Sahih Muslim"),
            collection("ibnmajah", "Sunan Ibn Majah"),
            collection("nasai", "Sunan an-Nasa'i")
        )

        val sections = sectionedHadithCollections(collections)

        assertEquals(HadithCollectionSection.KUTUBUS_SITTAH, sections[0].first)
        assertEquals(
            listOf("bukhari", "muslim", "abudawud", "tirmidhi", "nasai", "ibnmajah"),
            sections[0].second.map(HadithCollection::id)
        )
        assertEquals(HadithCollectionSection.OTHER, sections[1].first)
        assertEquals(listOf("riyad_assalihin"), sections[1].second.map(HadithCollection::id))
    }

    @Test
    fun GIVEN_collection_without_local_rows_WHEN_grouping_THEN_item_is_retained_as_unavailable() {
        val collection = collection("riyad_assalihin", "Riyad as-Salihin", hasLocalContent = false)

        val item = sectionedHadithCollections(listOf(collection)).single().second.single()

        assertTrue(!item.hasLocalContent)
    }

    private fun collection(
        id: String,
        title: String,
        hasLocalContent: Boolean = true
    ) = HadithCollection(
        id = id,
        title = title,
        count = 1,
        hasLocalContent = hasLocalContent
    )
}
