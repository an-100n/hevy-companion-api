package com.hevycompanion.api.user.controller

import com.hevycompanion.api.user.dto.HevyKeyRequest
import com.hevycompanion.api.user.dto.UserProfileResponse
import com.hevycompanion.api.user.service.UserService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/users")
class UserController(private val userService: UserService) {

    @PostMapping("/sync")
    fun syncProfile(authentication: Authentication): ResponseEntity<UserProfileResponse> {
        val userId = UUID.fromString(authentication.name)
        return ResponseEntity.ok(userService.syncProfile(userId))
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
