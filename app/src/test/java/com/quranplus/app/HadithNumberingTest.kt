package com.quranplus.app

import com.quranplus.app.core.database.dao.HadithDao
import com.quranplus.app.core.database.entity.HadithEntity
import com.quranplus.app.features.hadith.data.HadithBundleManifest
import com.quranplus.app.features.hadith.data.HadithNumbering
import com.quranplus.app.features.hadith.data.HadithRepositoryImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class HadithNumberingTest {
    private val numbering = HadithNumbering { assetJson }
    private val dates = row(3340)
    private val other = row(3837)

    @Test
    fun GIVEN_archive3340_WHEN_resolvingReference_THEN_3837DescribesDatesAndButter() {
        assertEquals(3837, numbering.referenceNumber(dates))
        assertEquals(listOf(3340), numbering.archiveNumbers("abudawud", 3837))
        assertEquals("https://sunnah.com/abudawud:3837", numbering.canonicalUrl(dates))
        assertTrue(numbering.indonesianTranslation(dates).contains("mentega dan kurma"))
        assertTrue(numbering.reference(dates).contains("edisi tafsirq/gading no. 3340"))
        assertEquals(3_003_340L, dates.id)
    }

    @Test
    fun GIVEN_archive3837_WHEN_resolvingReference_THEN_neverLabelsItAsCanonical3837() {
        assertEquals(4420, numbering.referenceNumber(other))
        assertEquals("https://sunnah.com/abudawud:4420", numbering.canonicalUrl(other))
        assertTrue(numbering.numberLabel(other).contains("4420"))
    }

    @Test
    fun GIVEN_changedArabicOrTranslation_WHEN_resolving_THEN_rejectsContentMismatch() {
        listOf(dates.copy(textArabic = other.textArabic), dates.copy(translationId = other.translationId)).forEach {
            assertNull(numbering.referenceNumber(it))
            assertNull(numbering.canonicalUrl(it))
            assertTrue(numbering.reference(it).contains("belum dipetakan"))
        }
    }

    @Test
    fun GIVEN_unverifiedOrUnmappedRow_WHEN_displaying_THEN_identifiesArchiveNumber() {
        assertNull(numbering.referenceNumber(dates.copy(sourceSha256 = "a".repeat(64))))
        assertTrue(numbering.numberLabel(dates.copy(hadithNumber = 99999)).contains("nomor arsip"))
    }

    @Test
    fun GIVEN_canonical3837_WHEN_searching_THEN_looksUpArchive3340AndPreservesItsIdentity() = runTest {
        val dao = mockk<HadithDao>()
        coEvery { dao.getHadithByNumber("abudawud", 3340) } returns dates
        val records = HadithRepositoryImpl(dao, numbering).search("abudawud", "3837")
        assertEquals(listOf(3340), records.map { it.hadithNumber })
        assertEquals(listOf(3837), records.map { it.referenceNumber })
        assertTrue(records.single().translationId.contains("mentega dan kurma"))
        coVerify(exactly = 0) { dao.searchByNumber(any(), 3837, any()) }
    }

    @Test
    fun GIVEN_existingArchiveBookmark_WHEN_loadingCollection_THEN_keepsOriginalNumbersAndIds() = runTest {
        val dao = mockk<HadithDao>()
        every { dao.getHadithsByCollection("abudawud") } returns flowOf(listOf(dates, other))
        val records = HadithRepositoryImpl(dao, numbering).search("abudawud", "")
        assertEquals(listOf(dates.id, other.id), records.map { it.id })
        assertEquals(listOf(3340, 3837), records.map { it.hadithNumber })
    }

    @Test
    fun GIVEN_explicitArchiveNumber_WHEN_searching_THEN_keepsThatEditionInsteadOfSubstituting3837() = runTest {
        val dao = mockk<HadithDao>()
        coEvery { dao.searchByNumber("abudawud", 3837, any()) } returns listOf(other)
        val records = HadithRepositoryImpl(dao, numbering).search("abudawud", "arsip 3837")
        assertEquals(3837, records.single().hadithNumber)
        assertEquals(4420, records.single().referenceNumber)
        coVerify(exactly = 0) { dao.getHadithByNumber(any(), any()) }
    }

    @Test
    fun GIVEN_modifiedExtractedBook_WHEN_claimedAsVerified_THEN_checksumRejectsIt() {
        assertFalse(HadithBundleManifest.VERIFIED.verifyBookJson("abu-daud.json", fixture.toString()))
        assertFalse(HadithBundleManifest.VERIFIED.verifyBookJson("foreign.json", "[]"))
    }

    companion object {
        private val fixture: JSONArray by lazy {
            JSONArray(HadithNumberingTest::class.java.getResource("/hadith/abu-dawud-numbering.json")!!.readText())
        }

        fun row(number: Int): HadithEntity {
        val record = (0 until fixture.length()).map(fixture::getJSONObject).first { it.getInt("number") == number }
        return HadithEntity(
            id = 3_000_000L + number, collectionId = "abudawud", hadithNumber = number,
            title = "Sunan Abu Dawud", textArabic = record.getString("arab"),
            translationId = record.getString("id"), translationEn = "", reference = "Sunan Abu Dawud no. $number",
            sourceRevision = HadithBundleManifest.VERIFIED.revision,
            sourceSha256 = HadithBundleManifest.VERIFIED.archiveSha256,
            licenseStatus = "licensed", language = "id", isComplete = true
        )
        }

        val assetJson: String by lazy {
            listOf(File("src/main/assets/hadith/numbering.json"), File("app/src/main/assets/hadith/numbering.json"))
                .first(File::isFile).readText()
        }
    }
}
