package com.hevycompanion.api.hevy.controller

import com.hevycompanion.api.hevy.client.HevyClient
import com.hevycompanion.api.hevy.dto.HevyRoutinesResponse
import com.hevycompanion.api.hevy.dto.HevyWorkout
import com.hevycompanion.api.hevy.dto.HevyWorkoutsResponse
import com.hevycompanion.api.hevy.service.HevySyncService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/hevy")
class HevyProxyController(
    private val hevyClient: HevyClient,
    private val hevySyncService: HevySyncService
) {

    @PostMapping("/sync-dictionary")
    fun syncDictionary(authentication: Authentication): ResponseEntity<Map<String, String>> {
        val userId = UUID.fromString(authentication.name)
        hevySyncService.syncExerciseDictionary(userId)
        return ResponseEntity.ok(mapOf("message" to "Exercise dictionary synchronized successfully"))
    }

    @GetMapping("/routines")
    fun getRoutines(authentication: Authentication): ResponseEntity<HevyRoutinesResponse> {
        val userId = UUID.fromString(authentication.name)
        return ResponseEntity.ok(hevyClient.getRoutines(userId))
    }

    @GetMapping("/workouts")
    fun getWorkouts(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "10") pageSize: Int,
        authentication: Authentication
    ): ResponseEntity<HevyWorkoutsResponse> {
        val userId = UUID.fromString(authentication.name)
        return ResponseEntity.ok(hevyClient.getWorkouts(userId, page, pageSize))
    }

    @GetMapping("/workouts/{workoutId}")
    fun getWorkout(
        @PathVariable workoutId: String,
        authentication: Authentication
    ): ResponseEntity<HevyWorkout> {
        val userId = UUID.fromString(authentication.name)
        return ResponseEntity.ok(hevyClient.getWorkout(userId, workoutId))
    }
}
