package com.hevycompanion.api.user.entity

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "users")
class User(
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID,

    @Column(nullable = false, unique = true)
    val email: String,

    @Column(name="hevy_username" ,nullable = false, unique = true)
    val username: String?,

    @Column(nullable = false)
    var timezone: String = "UTC",

    @Column(name = "hevy_api_key")
    var hevyApiKey: String? = null, // Encrypted string

    @Column(name = "hevy_id")
    var hevyId: UUID? = null,

    @Column(name = "hevy_url", columnDefinition = "text")
    var hevyUrl: String? = null
)