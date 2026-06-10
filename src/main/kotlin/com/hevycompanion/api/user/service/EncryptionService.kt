package com.hevycompanion.api.user.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.encrypt.Encryptors
import org.springframework.security.crypto.encrypt.TextEncryptor
import org.springframework.stereotype.Service


@Service
class EncryptionService(
    @Value($$"${HEVY_ENCRYPTION_SECRET}") password: String,
    @Value($$"${HEVY_ENCRYPTION_SALT}") salt: String
) {
    private val encryptor: TextEncryptor = Encryptors.delux(password, salt)

    fun encrypt(plainText: String?): String? =
        plainText?.takeIf { it.isNotBlank() }?.let { encryptor.encrypt(it) }

    fun decrypt(encryptedText: String?): String? =
        encryptedText?.takeIf { it.isNotBlank() }?.let { encryptor.decrypt(it) }
}


//@Service
//class EncryptionService(@Value($$"${HEVY_ENCRYPTION_SECRET}") secret: String) {
//
//
//    companion object {
//        private const val ALGORITHM = "AES"
//        private const val TRANSFORMATION = "AES/GCM/NoPadding"
//        private const val GCM_IV_LENGTH = 12
//        private const val GCM_TAG_LENGTH = 128
//    }
//    // Pad or truncate the secret to exactly 32 bytes for AES-256
//    private val secretKeySpec = SecretKeySpec(secret.toByteArray(Charsets.UTF_8).copyOf(32), ALGORITHM)
//
//
//    fun encrypt(plainText: String?): String? {
//        if (plainText.isNullOrBlank()) return null
//
//        val iv = ByteArray(GCM_IV_LENGTH).also { SecureRandom().nextBytes(it) }
//        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
//            init(Cipher.ENCRYPT_MODE, secretKeySpec, GCMParameterSpec(GCM_TAG_LENGTH, iv))
//        }
//        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
//
//        // Prepend the 12-byte IV to the ciphertext so we can extract it during decryption
//        return Base64.getEncoder().encodeToString(iv + encryptedBytes)
//    }
//
//    fun decrypt(encryptedText: String?): String? {
//        if (encryptedText.isNullOrBlank()) return null
//
//        val combined = Base64.getDecoder().decode(encryptedText)
//        val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
//        val encryptedBytes = combined.copyOfRange(GCM_IV_LENGTH, combined.size)
//
//        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
//            init(Cipher.DECRYPT_MODE, secretKeySpec, GCMParameterSpec(GCM_TAG_LENGTH, iv))
//        }
//
//        return String(cipher.doFinal(encryptedBytes), Charsets.UTF_8)
//    }
//}
