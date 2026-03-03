package com.hevycompanion.api.user.controller

import com.hevycompanion.api.user.dto.HevyKeyRequest
import com.hevycompanion.api.user.dto.UserProfileResponse
import com.hevycompanion.api.user.entity.User
import com.hevycompanion.api.user.repository.UserRepository
import com.hevycompanion.api.user.service.UserService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userRepository: UserRepository,
    private val userService: UserService
) {

    @PostMapping("/sync")
    suspend fun syncProfile(authentication: Authentication): User {
        val userId = UUID.fromString(authentication.name) // The 'sub' UUID

        return userRepository.findById(userId).orElseGet {
            // Logic to create the new "Shadow Profile"
            userRepository.save(
                User(
                    id = userId,
                    email = "placeholder@hevycompanion.com", // You'll likely want to extract email from JWT later
                    username = "User_${userId.toString().take(5)}"
                )
            )
        }
    }

    @GetMapping("/profile")
    fun getProfile(authentication: Authentication): ResponseEntity<UserProfileResponse> {
        val userId = UUID.fromString(authentication.name)
        return ResponseEntity.ok(userService.getUserProfile(userId))
    }

    @PutMapping("/hevy-key")
    fun updateHevyKey(
        @RequestBody @Valid request: HevyKeyRequest,
        authentication: Authentication
    ): ResponseEntity<Map<String, String>> {
        val userId = UUID.fromString(authentication.name)
        userService.saveHevyKey(userId, request.apiKey)
        return ResponseEntity.ok(mapOf("message" to "Hevy API key updated successfully"))
    }
}