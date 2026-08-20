package com.quranplus.app

import com.quranplus.app.features.hadith.data.HadithBundleImporter
import com.quranplus.app.features.hadith.data.HadithReferenceImporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HadithBundleContractTest {
    @Test
    fun GIVEN_bundleSource_WHEN_contractIsRead_THEN_realBookFilesAndSourceAreDeclared() {
        assertTrue(HadithBundleImporter.BUNDLE_URL.startsWith("https://codeload.github.com/"))
        assertEquals(9, HadithReferenceImporter.BUNDLE_BOOK_NAMES.size)
        assertTrue("bukhari.json" in HadithReferenceImporter.BUNDLE_BOOK_NAMES)
        assertTrue("tirmidzi.json" in HadithReferenceImporter.BUNDLE_BOOK_NAMES)
    }

    @Test
    fun GIVEN_sourceFileName_WHEN_collectionIdIsResolved_THEN_aliasesUseAppIds() {
        assertEquals("abudawud", HadithReferenceImporter.collectionIdFromFileName("abu-daud.json"))
        assertEquals("ibnmajah", HadithReferenceImporter.collectionIdFromFileName("ibnu-majah.json"))
        assertEquals("tirmidhi", HadithReferenceImporter.collectionIdFromFileName("tirmidzi.json"))
    }
}
