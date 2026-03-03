package com.hevycompanion.api.hevy.controller

import com.hevycompanion.api.hevy.client.HevyClient
import com.hevycompanion.api.hevy.service.HevySyncService
import com.hevycompanion.api.hevy.dto.HevyRoutinesResponse
import com.hevycompanion.api.hevy.dto.HevyWorkout
import com.hevycompanion.api.hevy.dto.HevyWorkoutsResponse
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
        
        return try {
            hevySyncService.syncExerciseDictionary(userId)
            ResponseEntity.ok(mapOf("message" to "Exercise dictionary synchronized successfully"))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Failed to sync dictionary")))
        }
    }

    @GetMapping("/routines")
    fun getRoutines(authentication: Authentication): ResponseEntity<HevyRoutinesResponse> {
        val userId = UUID.fromString(authentication.name)
        
        return try {
            val routines = hevyClient.getRoutines(userId)
            ResponseEntity.ok(routines)
        } catch (e: IllegalStateException) {
            ResponseEntity.badRequest().build()
        }
    }

    @GetMapping("/workouts")
    fun getWorkouts(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "10") pageSize: Int,
        authentication: Authentication
    ): ResponseEntity<HevyWorkoutsResponse> {
        val userId = UUID.fromString(authentication.name)
        
        return try {
            val workouts = hevyClient.getWorkouts(userId, page, pageSize)
            ResponseEntity.ok(workouts)
        } catch (e: IllegalStateException) {
            ResponseEntity.badRequest().build()
        }
    }

    @GetMapping("/workouts/{workoutId}")
    fun getWorkout(
        @PathVariable workoutId: String,
        authentication: Authentication
    ): ResponseEntity<HevyWorkout> {
        val userId = UUID.fromString(authentication.name)
        
        return try {
            val workout = hevyClient.getWorkout(userId, workoutId)
            ResponseEntity.ok(workout)
        } catch (e: IllegalStateException) {
            ResponseEntity.badRequest().build()
        }
    }
}