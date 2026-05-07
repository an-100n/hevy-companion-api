package com.hevycompanion.api.aichat.controller

import com.hevycompanion.api.aichat.service.HevyAnalysisService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import org.springframework.http.MediaType
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/chat")
class ChatController(private val analysisService: HevyAnalysisService) {

    @GetMapping("/analyze/routine/{routineId}", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun analyzeRoutine(
        @PathVariable routineId: UUID,
        authentication: Authentication
    ): Flow<String> {
        val userId = UUID.fromString(authentication.name)
        return analysisService.analyzeRoutineHistory(userId, routineId)
            .catch { e -> emit("\n\n[Coaching Error: ${e.message}]") }
    }

    @GetMapping("/analyze/workout-history/{workoutId}", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun analyzeWorkoutHistory(
        @PathVariable workoutId: String,
        authentication: Authentication
    ): Flow<String> {
        val userId = UUID.fromString(authentication.name)
        return analysisService.analyzeWorkoutHistory(userId, workoutId)
            .catch { e -> emit("\n\n[Error: ${e.message}]") }
    }
}
