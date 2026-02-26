package com.hevycompanion.api.user.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.util.UUID

@Entity
@Table(name = "users")
class User(
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "username", nullable = false, unique = true)
    val dbUsername: String = "",

    @Column(nullable = false, unique = true)
    val email: String = "",

    @Column(name = "password", nullable = false)
    val dbPassword: String = "",

    @Column(nullable = false)
    var timezone: String = "UTC",

    @Enumerated(EnumType.STRING)
    @Column(name = "sex", nullable = true)
    var sex: Sex? = null,

    @Column(name = "height_cm", nullable = true)
    var heightCm: Double? = null,

    @Column(name = "weight_kg", nullable = true)
    var weightKg: Double? = null,

    @Column(name = "birth_date", nullable = true)
    var birthDate: LocalDate? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
) : UserDetails { // Added the missing colon and space

    fun getAge(): Int? {
        return birthDate?.let { Period.between(it, LocalDate.now()).years }
    }

    override fun getAuthorities(): MutableCollection<out GrantedAuthority> =
        mutableListOf(SimpleGrantedAuthority("ROLE_USER"))

    override fun getPassword(): String = dbPassword
    override fun getUsername(): String = dbUsername

    override fun isAccountNonExpired(): Boolean = true
    override fun isAccountNonLocked(): Boolean = true
    override fun isCredentialsNonExpired(): Boolean = true
    override fun isEnabled(): Boolean = true

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is User) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "User(id=$id, username='$dbUsername')"
}