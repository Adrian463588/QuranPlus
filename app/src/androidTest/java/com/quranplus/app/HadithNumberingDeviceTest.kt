package com.quranplus.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.quranplus.app.core.database.QuranDatabase
import com.quranplus.app.features.hadith.data.HadithNumbering
import com.quranplus.app.features.hadith.data.HadithRepositoryImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Read-only acceptance against the user's installed nine-book corpus. */
@RunWith(AndroidJUnit4::class)
class HadithNumberingDeviceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val dao = QuranDatabase.getInstance(context).hadithDao()
    private val numbering = HadithNumbering {
        context.assets.open("hadith/numbering.json").bufferedReader().use { it.readText() }
    }

    @Test
    fun GIVEN_installedArchive_WHEN_searching3837_THEN_datesAndButterWithOriginalIdentity() = runBlocking {
        val count = dao.countHadiths()
        val original = dao.getHadithByNumber("abudawud", 3340) ?: error("Install the real hadith bundle first")
        val records = HadithRepositoryImpl(dao, numbering).search("abudawud", "3837")
        assertEquals(1, records.size)
        assertEquals(original.id, records.single().id)
        assertEquals(3340, records.single().hadithNumber)
        assertEquals(3837, records.single().referenceNumber)
        assertTrue(records.single().translationId.contains("mentega dan kurma"))
        assertEquals(count, dao.countHadiths())
    }

    @Test
    fun GIVEN_oldArchive3837_WHEN_reading_THEN_referenceIs4420WithoutRewritingTheRow() = runBlocking {
        val original = dao.getHadithByNumber("abudawud", 3837) ?: error("Install the real hadith bundle first")
        assertEquals(4420, numbering.referenceNumber(original))
        assertEquals("https://sunnah.com/abudawud:4420", numbering.canonicalUrl(original))
        assertEquals(original, dao.getHadithByNumber("abudawud", 3837))
    }

    @Test
    fun GIVEN_explicitArchiveNumber_WHEN_searching_THEN_canonicalReferenceKeepsCorrectNarration() = runBlocking {
        val records = HadithRepositoryImpl(dao, numbering).search("abudawud", "arsip 3837")
        assertEquals(3837, records.single().hadithNumber)
        assertEquals(4420, records.single().referenceNumber)
        assertTrue(records.single().reference.contains("edisi tafsirq/gading no. 3837"))
    }
}
