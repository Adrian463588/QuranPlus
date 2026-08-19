package com.quranplus.app

import com.quranplus.app.core.audio.EveryAyahAudioSource
import com.quranplus.app.core.audio.Qari
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioSourceCatalogTest {

    @Test
    fun GIVEN_qariAndAyah_WHEN_buildingEveryAyahUrl_THEN_usesVerifiedVersePath() {
        val descriptor = EveryAyahAudioSource.descriptor(Qari.MISHARY_ALAFASY)

        assertEquals("001001.mp3", descriptor.fileName(1, 1))
        assertEquals(
            "https://everyayah.com/data/Alafasy_128kbps/001001.mp3",
            descriptor.audioUrl(1, 1)
        )
        assertTrue(descriptor.checksumUrl.endsWith("/Alafasy_128kbps/000_checksum.md5"))
    }

    @Test
    fun GIVEN_everyAyahChecksumText_WHEN_parsing_THEN_readsOnlyValidMd5Entries() {
        val checksums = EveryAyahAudioSource.parseChecksums(
            """
            92cd0474a1073e264fac310fb0c28351 *001001.mp3
            invalid line
            11848d9c4bc843a8c382bd069c5829ca 001002.mp3
            """.trimIndent()
        )

        assertEquals(2, checksums.size)
        assertEquals("92cd0474a1073e264fac310fb0c28351", checksums["001001.mp3"])
        assertEquals("11848d9c4bc843a8c382bd069c5829ca", checksums["001002.mp3"])
    }

    @Test
    fun GIVEN_supportedQaris_WHEN_buildingDescriptors_THEN_allUseEveryAyahChecksumSource() {
        Qari.entries.forEach { qari ->
            val descriptor = EveryAyahAudioSource.descriptor(qari)

            assertTrue(descriptor.audioUrl(114, 6).startsWith("https://everyayah.com/data/"))
            assertTrue(descriptor.checksumUrl.endsWith("/000_checksum.md5"))
        }
    }
}
