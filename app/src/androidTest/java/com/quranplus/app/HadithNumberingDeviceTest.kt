package com.quranplus.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.quranplus.app.core.database.QuranDatabase
import com.quranplus.app.features.hadith.data.CanonicalHadithSynchronizer
import com.quranplus.app.features.hadith.data.HadithNumbering
import com.quranplus.app.features.hadith.data.HadithRepositoryImpl
import com.quranplus.app.features.hadith.domain.HadithLookupStatus
import com.quranplus.app.features.hadith.domain.HadithNumberingScheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Acceptance verification against the live canonical and archive crosswalk database on device. */
@RunWith(AndroidJUnit4::class)
class HadithNumberingDeviceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val database = QuranDatabase.getInstance(context)
    private val dao = database.hadithDao()
    private val numbering = HadithNumbering {
        context.assets.open("hadith/numbering.json").bufferedReader().use { it.readText() }
    }
    private val synchronizer = CanonicalHadithSynchronizer(context, database)
    private val repository = HadithRepositoryImpl(dao, numbering, synchronizer)

    @Test
    fun GIVEN_canonicalSync_WHEN_searchingAbuDawud3837AndArsip3837_THEN_bothSchemesStayDistinct() = runBlocking {
        val canonical = repository.searchDetailed("abudawud", "3837", HadithNumberingScheme.SUNNAH)
        assertEquals(HadithLookupStatus.FOUND, canonical.status)
        val rec = canonical.records.single()
        assertEquals(3837, rec.referenceNumber)
        assertEquals("https://sunnah.com/abudawud:3837", rec.canonicalUrl)
        assertTrue(rec.translationId.contains("mentega dan kurma"))
        assertTrue(rec.alternateReferences.any { it.scheme == HadithNumberingScheme.ARCHIVE_GADING && it.number == 3340 })

        val archive = repository.searchDetailed("abudawud", "arsip 3837", HadithNumberingScheme.SUNNAH)
        assertEquals(HadithLookupStatus.FOUND, archive.status)
        val archRec = archive.records.single()
        assertEquals(3837, archRec.hadithNumber)
        assertEquals(4420, archRec.referenceNumber)
        assertEquals("https://sunnah.com/abudawud:4420", archRec.canonicalUrl)
    }

    @Test
    fun GIVEN_canonicalSync_WHEN_searchingAllFourGoldenReferencesOffline_THEN_everyReferenceResolvesAccurately() = runBlocking {
        // HF-001: Sahih al-Bukhari 3617
        val hf1 = repository.searchDetailed("bukhari", "3617", HadithNumberingScheme.SUNNAH)
        assertEquals(HadithLookupStatus.FOUND, hf1.status)
        assertTrue(hf1.records.single().textArabic.contains("نَصْرَانِيًّا"))
        assertTrue(hf1.records.single().translationEn.contains("Christian"))
        assertEquals("https://sunnah.com/bukhari:3617", hf1.records.single().canonicalUrl)

        // HF-002: Sunan Abi Dawud 4000
        val hf2 = repository.searchDetailed("abudawud", "4000", HadithNumberingScheme.SUNNAH)
        assertEquals(HadithLookupStatus.FOUND, hf2.status)
        assertTrue(hf2.records.single().textArabic.contains("مَالِكِ يَوْمِ الدِّينِ"))
        assertTrue(hf2.records.single().textArabic.contains("مَلِكِ يَوْمِ الدِّينِ"))
        assertTrue(hf2.records.single().grades.any { it.authority == "Al-Albani" })
        assertEquals("https://sunnah.com/abudawud:4000", hf2.records.single().canonicalUrl)

        // HF-003: Jami' at-Tirmidhi 3000
        val hf3 = repository.searchDetailed("tirmidhi", "3000", HadithNumberingScheme.SUNNAH)
        assertEquals(HadithLookupStatus.FOUND, hf3.status)
        assertTrue(hf3.records.single().textArabic.contains("أَبُو أُمَامَةَ"))
        assertTrue(hf3.records.single().translationEn.contains("Khawarij"))
        assertFalse(hf3.records.single().translationEn.contains("Hajj"))

        // HF-004: Jami' at-Tirmidhi 813
        val hf4 = repository.searchDetailed("tirmidhi", "813", HadithNumberingScheme.SUNNAH)
        assertEquals(HadithLookupStatus.FOUND, hf4.status)
        assertTrue(hf4.records.single().textArabic.contains("الزَّادُ وَالرَّاحِلَةُ"))
        assertTrue(hf4.records.single().translationEn.contains("provisions"))
        assertTrue(hf4.records.single().translationEn.contains("Hajj"))
        assertNotEquals(hf3.records.single().stableId, hf4.records.single().stableId)

        // HF-005: Musnad Ahmad 4001 (Ar-Risalah vs Al-Alamiyah/Arsip)
        val hf5Ris = repository.searchDetailed("ahmad", "4001", HadithNumberingScheme.AR_RISALAH)
        assertEquals(HadithLookupStatus.FOUND, hf5Ris.status)
        assertTrue(hf5Ris.records.single().textArabic.contains("عَشِيَّةَ الْجُمُعَةِ"))
        assertTrue(hf5Ris.records.single().textArabic.contains("آيَةُ اللِّعَانِ"))
        assertTrue(hf5Ris.records.single().translationId.contains("ayat li'an"))
        assertEquals("https://hadits.net/ahmad:4001", hf5Ris.records.single().canonicalUrl)

        val hf5Al = repository.searchDetailed("ahmad", "4001", HadithNumberingScheme.ARCHIVE_GADING)
        assertEquals(HadithLookupStatus.FOUND, hf5Al.status)
        assertTrue(hf5Al.records.single().textArabic.contains("مَنْ نَزَلَتْ بِهِ فَاقَةٌ"))
        assertNotEquals(hf5Ris.records.single().stableId, hf5Al.records.single().stableId)
    }
}
