package com.quranplus.app.features.hadith.data

import com.quranplus.app.core.database.entity.HadithEntity
import java.io.File
import java.security.MessageDigest

/** Immutable release contract for the hadith bundle used by the offline RAG. */
data class HadithBundleManifest(
    val bundleId: String,
    val revision: String,
    val archiveUrl: String,
    val sourceUrl: String,
    val archiveSizeBytes: Long,
    val archiveSha256: String,
    val licenseId: String,
    val licenseUrl: String,
    val expectedCollectionCounts: Map<String, Int> = emptyMap()
) {
    fun verifyArchive(file: File): Boolean {
        if (!file.isFile || file.length() != archiveSizeBytes) return false
        return sha256(file).equals(archiveSha256, ignoreCase = true)
    }

    fun isVerifiedRecord(record: HadithEntity): Boolean =
        record.collectionId in expectedCollectionCounts &&
            record.isComplete &&
            record.sourceRevision == revision &&
            record.sourceSha256.equals(archiveSha256, ignoreCase = true) &&
            record.licenseStatus.equals(VERIFIED_RECORD_LICENSE_STATUS, ignoreCase = true)

    fun isVerifiedCorpus(collectionCounts: Map<String, Int>): Boolean =
        expectedCollectionCounts.isNotEmpty() && collectionCounts == expectedCollectionCounts

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }

    companion object {
        private const val VERIFIED_RECORD_LICENSE_STATUS = "licensed"

        /** Pinned GitHub commit, measured from the immutable codeload archive. */
        val VERIFIED = HadithBundleManifest(
            bundleId = "gadingnst-hadith-api",
            revision = "8e15b4f9e7585822426a0d470e8dfec27e2a1707",
            archiveUrl = "https://codeload.github.com/gadingnst/hadith-api/zip/8e15b4f9e7585822426a0d470e8dfec27e2a1707",
            sourceUrl = "https://github.com/gadingnst/hadith-api/tree/8e15b4f9e7585822426a0d470e8dfec27e2a1707",
            archiveSizeBytes = 15_036_414L,
            archiveSha256 = "1f7ffe09ccf0be44293d3b68de036a37d4c54fc36d7f81390d8e5faeed547b1c",
            licenseId = "MIT",
            licenseUrl = "https://opensource.org/licenses/MIT",
            expectedCollectionCounts = mapOf(
                "abudawud" to 4_419,
                "ahmad" to 4_305,
                "bukhari" to 6_638,
                "darimi" to 2_949,
                "ibnmajah" to 4_285,
                "malik" to 1_587,
                "muslim" to 4_930,
                "nasai" to 5_364,
                "tirmidhi" to 3_625
            )
        )
    }
}

data class VerifiedHadithSource(
    val revision: String,
    val licenseId: String,
    val licenseUrl: String,
    val sourceSha256: String? = null
)
