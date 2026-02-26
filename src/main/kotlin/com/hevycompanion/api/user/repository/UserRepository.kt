package com.hevycompanion.api.user.repository

import com.hevycompanion.api.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface UserRepository : JpaRepository<User, UUID> {
    fun findByEmail(email: String): User?
    fun findByDbUsername(dbUsername: String): User?
    fun existsByEmail(email: String): Boolean
    fun existsByDbUsername(dbUsername: String): Boolean
}