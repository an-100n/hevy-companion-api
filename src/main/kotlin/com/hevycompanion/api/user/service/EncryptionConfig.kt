package com.hevycompanion.api.user.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
class EncryptionConfig {
    @Value("\${HEVY_ENCRYPTION_SECRET}")
    lateinit var secret: String
}