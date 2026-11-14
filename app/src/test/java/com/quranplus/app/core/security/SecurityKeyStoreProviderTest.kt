package com.quranplus.app.core.security

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.KeyGenerator
import javax.crypto.AEADBadTagException

class SecurityKeyStoreProviderTest {

    private fun createTestProvider(): SecurityKeyStoreProvider {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        val testKey = keyGen.generateKey()
        return SecurityKeyStoreProvider(fallbackKeyForTesting = testKey)
    }

    @Test
    fun encryptAndDecrypt_bytePayload_restoresOriginalContent() {
        val provider = createTestProvider()
        val original = "Surah Al-Baqarah 286: Secret Marker Data".toByteArray(Charsets.UTF_8)

        val encrypted = provider.encrypt(original)
        assertTrue(encrypted.size > 12 + original.size)

        val decrypted = provider.decrypt(encrypted)
        assertArrayEquals(original, decrypted)
    }

    @Test
    fun encryptAndDecrypt_string_restoresOriginalString() {
        val provider = createTestProvider()
        val message = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ - QuranPlus Offline Protected Data"

        val encrypted = provider.encryptString(message)
        val decrypted = provider.decryptString(encrypted)

        assertEquals(message, decrypted)
    }

    @Test
    fun encrypt_calledTwice_generatesDistinctIvAndCiphertext() {
        val provider = createTestProvider()
        val plain = "Consistent Input".toByteArray(Charsets.UTF_8)

        val encrypted1 = provider.encrypt(plain)
        val encrypted2 = provider.encrypt(plain)

        // Random 12-byte IV must make ciphertexts distinct
        assertNotEquals(encrypted1.take(12), encrypted2.take(12))
    }

    @Test
    fun decrypt_tamperedCiphertext_throwsException() {
        val provider = createTestProvider()
        val message = "Sensitive data"
        val encrypted = provider.encryptString(message)

        // Flip a byte in the ciphertext payload
        encrypted[encrypted.size - 1] = (encrypted[encrypted.size - 1].toInt() xor 0xFF).toByte()

        assertThrows(Exception::class.java) {
            provider.decrypt(encrypted)
        }
    }
}
