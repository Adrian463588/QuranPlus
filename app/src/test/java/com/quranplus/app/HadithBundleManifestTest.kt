package com.quranplus.app

import com.quranplus.app.core.database.entity.HadithEntity
import com.quranplus.app.features.hadith.data.HadithBundleManifest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HadithBundleManifestTest {

    @Test
    fun GIVEN_rowFromVerifiedArchive_WHEN_checkedForGrounding_THEN_rowIsAccepted() {
        val row = hadithRow(
            sourceSha256 = HadithBundleManifest.VERIFIED.archiveSha256
        )

        assertTrue(HadithBundleManifest.VERIFIED.isVerifiedRecord(row))
    }

    @Test
    fun GIVEN_rowWithExtractedBookDigest_WHEN_checkedForGrounding_THEN_rowIsRejected() {
        val row = hadithRow(sourceSha256 = "a".repeat(64))

        assertFalse(HadithBundleManifest.VERIFIED.isVerifiedRecord(row))
    }

    private fun hadithRow(sourceSha256: String) = HadithEntity(
        id = 1L,
        collectionId = "bukhari",
        hadithNumber = 1,
        title = "Sahih al-Bukhari",
        textArabic = "نص الحديث",
        translationId = "Teks hadist",
        translationEn = "Hadith text",
        reference = "Sahih al-Bukhari no. 1",
        sourceRevision = HadithBundleManifest.VERIFIED.revision,
        sourceSha256 = sourceSha256,
        licenseStatus = "licensed",
        language = "id",
        isComplete = true
    )
}
