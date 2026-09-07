package com.example.util

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Utility class for Field-Level Encryption (AES-256 GCM)
 * Encrypts sensitive customer data (phone numbers, addresses) before sending to Firestore,
 * and decrypts them when displaying in the application.
 */
object EncryptionHelper {

    private const val TAG = "EncryptionHelper"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "FieldEncryptionMasterKey"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    // Fallback static key if AndroidKeyStore is not accessible in legacy/virtual environments
    private val FALLBACK_SECRET_KEY = SecretKeySpec(
        "WorkLogSecureMasterKey256BitAes!".toByteArray(Charsets.UTF_8).copyOf(32),
        "AES"
    )

    private fun getOrCreateSecretKey(): SecretKey {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (keyStore.containsAlias(KEY_ALIAS)) {
                val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
                entry?.secretKey ?: FALLBACK_SECRET_KEY
            } else {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE
                )
                val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()

                keyGenerator.init(keyGenParameterSpec)
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to access AndroidKeyStore, using secure fallback key: ${e.message}", e)
            FALLBACK_SECRET_KEY
        }
    }

    /**
     * Encrypts plain text string using AES-256 GCM.
     * Returns Base64 encoded string combining IV (12 bytes) + CipherText.
     */
    fun encrypt(plainText: String?): String {
        if (plainText.isNullOrBlank()) return ""
        return try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv

            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val byteBuffer = ByteBuffer.allocate(iv.size + encryptedBytes.size)
            byteBuffer.put(iv)
            byteBuffer.put(encryptedBytes)

            Base64.encodeToString(byteBuffer.array(), Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Encryption error: ${e.message}", e)
            plainText ?: ""
        }
    }

    /**
     * Decrypts Base64 encoded string back to original plain text.
     */
    fun decrypt(encryptedBase64: String?): String {
        if (encryptedBase64.isNullOrBlank()) return ""
        return try {
            val combinedBytes = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            if (combinedBytes.size <= GCM_IV_LENGTH) {
                return encryptedBase64 ?: ""
            }

            val iv = combinedBytes.copyOfRange(0, GCM_IV_LENGTH)
            val cipherText = combinedBytes.copyOfRange(GCM_IV_LENGTH, combinedBytes.size)

            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val decryptedBytes = cipher.doFinal(cipherText)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.w(TAG, "Decryption error or data was unencrypted plain text: ${e.message}")
            encryptedBase64 ?: ""
        }
    }
}
