package com.hevycompanion.api.user.service

import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Service
class EncryptionService(private val config: EncryptionConfig) {

    companion object {
        private const val ALGORITHM = "AES"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
    }

    private val secretKeySpec: SecretKeySpec by lazy {
        // Ensure the secret is exactly 32 bytes for AES-256
        val keyBytes = config.secret.toByteArray(Charsets.UTF_8).copyOf(32)
        SecretKeySpec(keyBytes, ALGORITHM)
    }

    fun encrypt(plainText: String?): String? {
        if (plainText.isNullOrBlank()) return null

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val iv = ByteArray(GCM_IV_LENGTH)
        SecureRandom().nextBytes(iv)
        val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)

        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, parameterSpec)
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        // We must store BOTH the IV and the Encrypted data to decrypt it later.
        // We prepend the 12-byte IV to the encrypted data.
        val combined = ByteArray(iv.size + encryptedBytes.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)

        return Base64.getEncoder().encodeToString(combined)
    }

    fun decrypt(encryptedText: String?): String? {
        if (encryptedText.isNullOrBlank()) return null

        val combined = Base64.getDecoder().decode(encryptedText)
        
        val iv = ByteArray(GCM_IV_LENGTH)
        System.arraycopy(combined, 0, iv, 0, iv.size)

        val encryptedBytes = ByteArray(combined.size - iv.size)
        System.arraycopy(combined, iv.size, encryptedBytes, 0, encryptedBytes.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, parameterSpec)
        val decryptedBytes = cipher.doFinal(encryptedBytes)
        
        return String(decryptedBytes, Charsets.UTF_8)
    }
}