package com.quranplus.app.core.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Hardware-backed cryptographic provider using AndroidKeyStore (AES-256-GCM).
 * Protects local sensitive data and preferences with authenticated encryption.
 */
class SecurityKeyStoreProvider(
    private val keyAlias: String = DEFAULT_KEY_ALIAS,
    private val fallbackKeyForTesting: SecretKey? = null
) {

    companion object {
        const val DEFAULT_KEY_ALIAS = "QuranPlusMasterKey"
        private const val ANDROID_KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val KEY_SIZE_BITS = 256
    }

    private val secretKey: SecretKey by lazy {
        fallbackKeyForTesting ?: getOrCreateKey(keyAlias)
    }

    /**
     * Encrypts plaintext bytes using AES-256-GCM.
     * Returns: 12-byte IV + Ciphertext (with authentication tag).
     */
    fun encrypt(plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv ?: error("Cipher IV was not generated")
        val encrypted = cipher.doFinal(plaintext)

        val buffer = ByteBuffer.allocate(iv.size + encrypted.size)
        buffer.put(iv)
        buffer.put(encrypted)
        return buffer.array()
    }

    /**
     * Decrypts ciphertext bytes produced by [encrypt].
     * Expects payload: 12-byte IV + Ciphertext (with authentication tag).
     */
    fun decrypt(encryptedPayload: ByteArray): ByteArray {
        require(encryptedPayload.size > GCM_IV_LENGTH) {
            "Payload too short to contain IV and ciphertext"
        }

        val iv = ByteArray(GCM_IV_LENGTH)
        val ciphertextLength = encryptedPayload.size - GCM_IV_LENGTH
        val ciphertext = ByteArray(ciphertextLength)

        val buffer = ByteBuffer.wrap(encryptedPayload)
        buffer.get(iv)
        buffer.get(ciphertext)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        return cipher.doFinal(ciphertext)
    }

    /**
     * Helper for strings.
     */
    fun encryptString(plaintext: String): ByteArray = encrypt(plaintext.toByteArray(Charsets.UTF_8))

    /**
     * Helper for strings.
     */
    fun decryptString(encryptedPayload: ByteArray): String = String(decrypt(encryptedPayload), Charsets.UTF_8)

    private fun getOrCreateKey(alias: String): SecretKey {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER).apply {
                load(null)
            }
            if (!keyStore.containsAlias(alias)) {
                generateKey(alias)
            }
            (keyStore.getEntry(alias, null) as KeyStore.SecretKeyEntry).secretKey
        } catch (_: Exception) {
            // JVM Unit test fallback if AndroidKeyStore provider is absent
            generateSoftwareKey()
        }
    }

    private fun generateKey(alias: String) {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE_PROVIDER
        )
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE_BITS)
            .build()

        keyGenerator.init(spec)
        keyGenerator.generateKey()
    }

    private fun generateSoftwareKey(): SecretKey {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(KEY_SIZE_BITS)
        return keyGen.generateKey()
    }
}
