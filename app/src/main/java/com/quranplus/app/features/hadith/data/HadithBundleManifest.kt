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

    fun verifyBookJson(filename: String, json: String): Boolean {
        val expected = BOOK_SHA256[filename.substringAfterLast('/').substringAfterLast('\\')] ?: return false
        val actual = MessageDigest.getInstance("SHA-256").digest(json.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
        return actual == expected
    }

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
        private val BOOK_SHA256 = mapOf(
            "abu-daud.json" to "b0efbb33a11aed7db639dc6e35f2bafc9882056fe113efca078c536051f74891",
            "ahmad.json" to "61cbd6b3fe8805982204f92598412fdc2241cbb4290ea2109fb9c53621f23258",
            "bukhari.json" to "65e7a48f8ae0b792e3f8374f9de3e45d3119a1fdb5b6322aa6182db585348bb8",
            "darimi.json" to "001408d019769e20615803a1f6ee6ee5b484e3d5128dd5b356fc9d510eae2089",
            "ibnu-majah.json" to "6ba481a76454a075282b4bc499c43d450596402be1a39762a1fa654321778531",
            "malik.json" to "bcd98f0230734921b00cafff253c2b52b0a1326efef7ef91fc5879468fde7edf",
            "muslim.json" to "c9dcee026176991c52c955628c7242f60a7ffafbb714c2cd387b14759bb89a71",
            "nasai.json" to "8114350778730353e5ff7a78cf4fdd2242610390160898efbc4bd7c537555560",
            "tirmidzi.json" to "91a70b0c36dcfc8e1cba248ae66fdd00075790f98eda2a8abcdbd72ee22b90fd"
        )

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
