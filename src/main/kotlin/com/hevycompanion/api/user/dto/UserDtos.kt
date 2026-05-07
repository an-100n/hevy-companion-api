package com.hevycompanion.api.user.dto

import jakarta.validation.constraints.NotBlank

data class HevyKeyRequest(
    @field:NotBlank(message = "Hevy API key cannot be blank")
    val apiKey: String
)

data class UserProfileResponse(
    val id: String,
    val email: String,
    val timezone: String,
    val hevyUsername: String?,
    val hasHevyKey: Boolean
)
