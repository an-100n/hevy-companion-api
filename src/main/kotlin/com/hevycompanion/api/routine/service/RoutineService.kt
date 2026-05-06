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
    fun getActiveRoutines(userId: UUID): List<ActiveRoutineResponse> =
        routineRepository.findByUserIdAndIsActiveTrue(userId).map {
            ActiveRoutineResponse(id = it.id.toString(), hevyRoutineId = it.hevyRoutineId.toString(), name = it.name)
        }

    @Transactional
    fun syncRoutines(userId: UUID, request: SyncRoutinesRequest) {
        val existingRoutines = routineRepository.findByUserId(userId)
        val existingByHevyId = existingRoutines.associateBy { it.hevyRoutineId }
        val selectedHevyIds = request.selectedRoutines.map { UUID.fromString(it.hevyRoutineId) }.toSet()

        existingRoutines
            .filter { it.hevyRoutineId !in selectedHevyIds }
            .forEach { it.isActive = false }

        request.selectedRoutines.forEach { selection ->
            val hevyId = UUID.fromString(selection.hevyRoutineId)
            val existing = existingByHevyId[hevyId]

            if (existing != null) {
                existing.isActive = true
                existing.name = selection.name
                existing.lastSyncedAt = Instant.now()
            } else {
                routineRepository.save(Routine(userId = userId, hevyRoutineId = hevyId, name = selection.name))
            }
        }
    }
}
