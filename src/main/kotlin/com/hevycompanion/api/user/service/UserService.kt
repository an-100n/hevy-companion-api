package com.hevycompanion.api.user.service

import com.hevycompanion.api.user.dto.UserProfileResponse
import com.hevycompanion.api.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UserService(
    private val userRepository: UserRepository,
    private val encryptionService: EncryptionService
) {

    @Transactional
    fun saveHevyKey(userId: UUID, rawKey: String) {
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }

        // 1. Encrypt the raw key
        val encryptedKey = encryptionService.encrypt(rawKey)
            ?: throw IllegalStateException("Failed to encrypt key")

        // 2. Save the cipher text to the database
        user.hevyApiKey = encryptedKey
        userRepository.save(user)
    }

    @Transactional(readOnly = true)
    fun getUserProfile(userId: UUID): UserProfileResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }

        return UserProfileResponse(
            id = user.id.toString(),
            email = user.email,
            timezone = user.timezone,
            hevyUsername = user.username,
            // SECURITY: Only return TRUE or FALSE, never the string itself!
            hasHevyKey = !user.hevyApiKey.isNullOrBlank()
        )
    }
}