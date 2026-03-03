package com.hevycompanion.api.routine.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "routines")
class Routine(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "hevy_routine_id", nullable = false)
    val hevyRoutineId: UUID,

    @Column(name = "name", nullable = false, columnDefinition = "text")
    var name: String,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "created_at")
    val createdAt: Instant = Instant.now(),

    @Column(name = "last_synced_at")
    var lastSyncedAt: Instant = Instant.now()
)