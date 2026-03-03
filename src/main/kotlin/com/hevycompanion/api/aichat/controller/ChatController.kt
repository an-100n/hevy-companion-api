package com.hevycompanion.api.aichat.controller

import com.hevycompanion.api.aichat.service.HevyAnalysisService
import org.springframework.ai.chat.client.ChatClient
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import java.util.UUID

@RestController
@RequestMapping("/api/v1/chat")
class ChatController(
    builder: ChatClient.Builder,
    private val analysisService: HevyAnalysisService
) {
    private val chatClient = builder.defaultSystem("You are an elite strength coach. Give concise, data-driven advice.")
        .build()

    @GetMapping()
    suspend fun chat(@RequestParam(value = "message", defaultValue = "Hello. Is bicep curl with dumbbells a good exercise?") message: String): String {
        return chatClient.prompt()
            .user(message)
            .call()
            .content() ?: "No response from AI"
    }

    // Notice the produces = MediaType.TEXT_EVENT_STREAM_VALUE
    // This tells the browser to keep the connection open and read chunks!
    @GetMapping("/analyze/{workoutId}", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun analyzeWorkout(
        @PathVariable workoutId: String,
        authentication: Authentication
    ): Flux<String> {
        val userId = UUID.fromString(authentication.name)
        
        return analysisService.analyzeWorkout(userId, workoutId)
            .onErrorResume { e -> 
                Flux.just("\n\n[Error: ${e.message}]") 
            }
    }

    @GetMapping("/analyze/routine/{routineId}", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun analyzeRoutine(
        @PathVariable routineId: UUID,
        authentication: Authentication
    ): Flux<String> {
        val userId = UUID.fromString(authentication.name)
        
        return analysisService.analyzeRoutineHistory(userId, routineId)
            .onErrorResume { e -> 
                Flux.just("\n\n[Error: ${e.message}]") 
            }
    }
}