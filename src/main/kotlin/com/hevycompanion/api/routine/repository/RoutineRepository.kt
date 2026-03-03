package com.hevycompanion.api.routine.repository

import com.hevycompanion.api.routine.entity.Routine
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface RoutineRepository : JpaRepository<Routine, UUID> {
    fun findByUserIdAndIsActiveTrue(userId: UUID): List<Routine>
    fun findByUserId(userId: UUID): List<Routine>
}