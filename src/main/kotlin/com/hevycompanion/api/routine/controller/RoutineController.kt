package com.hevycompanion.api.routine.controller

import com.hevycompanion.api.routine.dto.ActiveRoutineResponse
import com.hevycompanion.api.routine.dto.SyncRoutinesRequest
import com.hevycompanion.api.routine.service.RoutineService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/routines")
class RoutineController(private val routineService: RoutineService) {

    @GetMapping
    fun getActiveRoutines(authentication: Authentication): ResponseEntity<List<ActiveRoutineResponse>> {
        val userId = UUID.fromString(authentication.name)
        val routines = routineService.getActiveRoutines(userId)
        return ResponseEntity.ok(routines)
    }

    @PostMapping("/sync")
    fun syncRoutines(
        @RequestBody request: SyncRoutinesRequest,
        authentication: Authentication
    ): ResponseEntity<Map<String, String>> {
        val userId = UUID.fromString(authentication.name)
        routineService.syncRoutines(userId, request)
        return ResponseEntity.ok(mapOf("message" to "Routines synced successfully"))
    }
}