package com.hevycompanion.api.routine.service

import com.hevycompanion.api.routine.dto.ActiveRoutineResponse
import com.hevycompanion.api.routine.dto.SyncRoutinesRequest
import com.hevycompanion.api.routine.entity.Routine
import com.hevycompanion.api.routine.repository.RoutineRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class RoutineService(private val routineRepository: RoutineRepository) {

    @Transactional(readOnly = true)
    fun getActiveRoutines(userId: UUID): List<ActiveRoutineResponse> {
        return routineRepository.findByUserIdAndIsActiveTrue(userId).map {
            ActiveRoutineResponse(
                id = it.id.toString(),
                hevyRoutineId = it.hevyRoutineId.toString(),
                name = it.name
            )
        }
    }

    @Transactional
    fun syncRoutines(userId: UUID, request: SyncRoutinesRequest) {
        val existingRoutines = routineRepository.findByUserId(userId)
        
        // Convert the incoming string IDs to UUIDs
        val selectedHevyIds = request.selectedRoutines.map { UUID.fromString(it.hevyRoutineId) }.toSet()

        // 1. Deactivate routines that the user unselected
        existingRoutines.filter { it.hevyRoutineId !in selectedHevyIds }.forEach { 
            it.isActive = false 
        }

        // 2. Insert new routines OR reactivate existing ones
        request.selectedRoutines.forEach { selection ->
            val hevyId = UUID.fromString(selection.hevyRoutineId)
            val existingRoutine = existingRoutines.find { it.hevyRoutineId == hevyId }

            if (existingRoutine != null) {
                // It already exists in DB, just update it
                existingRoutine.isActive = true
                existingRoutine.name = selection.name
                existingRoutine.lastSyncedAt = Instant.now()
            } else {
                // Brand new selection, save to DB
                routineRepository.save(
                    Routine(
                        userId = userId,
                        hevyRoutineId = hevyId,
                        name = selection.name
                    )
                )
            }
        }
    }
}