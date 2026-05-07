package com.hevycompanion.api.user.service

import com.hevycompanion.api.user.dto.UserProfileResponse
import com.hevycompanion.api.user.entity.User
import com.hevycompanion.api.user.repository.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UserService(
    private val userRepository: UserRepository,
    private val encryptionService: EncryptionService
) {

    @Transactional
    fun syncProfile(userId: UUID): UserProfileResponse {
        val user = userRepository.findByIdOrNull(userId) ?: userRepository.save(
            User(
                id = userId,
                email = "placeholder@hevycompanion.com",
                username = "User_${userId.toString().take(5)}"
            )
        )
        return user.toProfileResponse()
    }

    @Transactional
    fun saveHevyKey(userId: UUID, rawKey: String) {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw IllegalArgumentException("User not found")

        user.hevyApiKey = encryptionService.encrypt(rawKey)
            ?: throw IllegalStateException("Failed to encrypt key")

        userRepository.save(user)
    }

    @Transactional(readOnly = true)
    fun getUserProfile(userId: UUID): UserProfileResponse {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw IllegalArgumentException("User not found")
        return user.toProfileResponse()
    }

    private fun User.toProfileResponse() = UserProfileResponse(
        id = id.toString(),
        email = email,
        timezone = timezone,
        hevyUsername = username,
        hasHevyKey = !hevyApiKey.isNullOrBlank()
    )
}
