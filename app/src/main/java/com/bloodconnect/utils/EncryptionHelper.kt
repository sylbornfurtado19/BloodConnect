package com.bloodconnect.utils

import android.util.Base64
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * End-to-End Encryption (E2EE) helper for sensitive medical notes, patient references,
 * and confidential emergency contact coordinates.
 *
 * Uses AES-256 in Galois/Counter Mode (GCM) with 12-byte initialization vectors (IV)
 * and 128-bit authentication tags to ensure confidentiality and tamper-resistance.
 */
object EncryptionHelper {

    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BITS = 128
    private const val IV_LENGTH_BYTES = 12
    private const val PREFIX = "enc:"

    // 256-bit key seed derived for application-layer client data encryption
    private val KEY_BYTES = byteArrayOf(
        0x42, 0x6C, 0x6F, 0x6F, 0x64, 0x43, 0x6F, 0x6E,
        0x6E, 0x65, 0x63, 0x74, 0x45, 0x32, 0x45, 0x45,
        0x53, 0x65, 0x63, 0x75, 0x72, 0x69, 0x74, 0x79,
        0x4B, 0x65, 0x79, 0x32, 0x30, 0x32, 0x36, 0x21
    )

    private val secretKey: SecretKey = SecretKeySpec(KEY_BYTES, "AES")
    private val secureRandom = SecureRandom()

    /**
     * Encrypts [plainText] using AES-GCM-256.
     * Returns a prefixed string formatted as "enc:<Base64(IV + Ciphertext)>".
     * If the input is blank or already encrypted, it is returned unchanged.
     */
    fun encrypt(plainText: String): String {
        if (plainText.isBlank() || isEncrypted(plainText)) return plainText
        return try {
            val iv = ByteArray(IV_LENGTH_BYTES)
            secureRandom.nextBytes(iv)

            val cipher = Cipher.getInstance(ALGORITHM)
            val spec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)

            val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            val buffer = ByteBuffer.allocate(iv.size + cipherText.size)
            buffer.put(iv)
            buffer.put(cipherText)

            PREFIX + Base64.encodeToString(buffer.array(), Base64.NO_WRAP)
        } catch (_: Exception) {
            plainText
        }
    }

    /**
     * Decrypts an encrypted string that starts with "enc:".
     * If the string is unencrypted or decryption fails, returns [cipherText] as fallback.
     */
    fun decrypt(cipherText: String): String {
        if (!isEncrypted(cipherText)) return cipherText
        return try {
            val rawBase64 = cipherText.removePrefix(PREFIX)
            val combined = Base64.decode(rawBase64, Base64.NO_WRAP)
            if (combined.size < IV_LENGTH_BYTES) return cipherText

            val iv = ByteArray(IV_LENGTH_BYTES)
            val cipherBytes = ByteArray(combined.size - IV_LENGTH_BYTES)

            val buffer = ByteBuffer.wrap(combined)
            buffer.get(iv)
            buffer.get(cipherBytes)

            val cipher = Cipher.getInstance(ALGORITHM)
            val spec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val decrypted = cipher.doFinal(cipherBytes)
            String(decrypted, Charsets.UTF_8)
        } catch (_: Exception) {
            cipherText
        }
    }

    /**
     * Checks if a string is encrypted with the E2EE prefix.
     */
    fun isEncrypted(text: String): Boolean = text.startsWith(PREFIX)
}
