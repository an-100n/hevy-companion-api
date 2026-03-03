package com.hevycompanion.api.hevy.client

import com.hevycompanion.api.hevy.dto.HevyRoutinesResponse
import com.hevycompanion.api.hevy.dto.HevyWorkout
import com.hevycompanion.api.user.repository.UserRepository
import com.hevycompanion.api.user.service.EncryptionService
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import com.hevycompanion.api.hevy.dto.HevyWorkoutsResponse
import com.hevycompanion.api.hevy.dto.HevyExerciseTemplatesResponse
import java.util.UUID


@Service
class HevyClient(
    private val userRepository: UserRepository,
    private val encryptionService: EncryptionService
) {

    private val restClient = RestClient.builder()
        .baseUrl("https://api.hevyapp.com/v1")
        .build()

    private fun getDecryptedApiKey(userId: UUID): String {
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }

        val encryptedKey = user.hevyApiKey
            ?: throw IllegalStateException("User has not configured a Hevy API key")

        return encryptionService.decrypt(encryptedKey)
            ?: throw IllegalStateException("Failed to decrypt Hevy API key")
    }

    fun getExerciseTemplates(userId: UUID, page: Int = 1, pageSize: Int = 100): HevyExerciseTemplatesResponse {
        val apiKey = getDecryptedApiKey(userId)

        return restClient.get()
            .uri("/exercise_templates?page={page}&pageSize={pageSize}", page, pageSize)
            .header("api-key", apiKey)
            .retrieve()
            .body(HevyExerciseTemplatesResponse::class.java)
            ?: throw RestClientException("Failed to fetch exercise templates: Response body was null")
    }

    fun getRoutines(userId: UUID): HevyRoutinesResponse {
        val apiKey = getDecryptedApiKey(userId)

        return restClient.get()
            .uri("/routines")
            .header("api-key", apiKey)
            .retrieve()
            .body(HevyRoutinesResponse::class.java)
            ?: throw RestClientException("Failed to fetch routines: Response body was null")
    }

    fun getWorkouts(userId: UUID, page: Int = 1, pageSize: Int = 10): HevyWorkoutsResponse {
        val apiKey = getDecryptedApiKey(userId)

        return restClient.get()
            .uri("/workouts?page={page}&pageSize={pageSize}", page, pageSize)
            .header("api-key", apiKey)
            .retrieve()
            .body(HevyWorkoutsResponse::class.java)
            ?: throw RestClientException("Failed to fetch workouts: Response body was null")
    }

    fun getWorkout(userId: UUID, workoutId: String): HevyWorkout {
        val apiKey = getDecryptedApiKey(userId)

        return restClient.get()
            .uri("/workouts/{id}", workoutId)
            .header("api-key", apiKey)
            .retrieve()
            .body(HevyWorkout::class.java)
            ?: throw RestClientException("Failed to fetch workout: Response body was null")
    }
}