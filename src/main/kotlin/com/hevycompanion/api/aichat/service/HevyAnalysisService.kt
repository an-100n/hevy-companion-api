package com.hevycompanion.api.aichat.service

import com.hevycompanion.api.hevy.client.HevyClient
import com.hevycompanion.api.hevy.dto.HevyWorkout
import com.hevycompanion.api.routine.repository.RoutineRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.withContext
import org.springframework.ai.chat.client.ChatClient
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.UUID


@Service
class HevyAnalysisService(
    builder: ChatClient.Builder,
    private val hevyClient: HevyClient,
    private val workoutContextService: WorkoutContextService,
    private val routineRepository: RoutineRepository
) {

    private val chatClient = builder.defaultSystem(
        """
        You are 'Pulse', an elite, data-driven strength and conditioning AI coach.
        You analyze raw workout logs and provide highly concise, actionable feedback.
        Focus on:
        1. Progressive overload (did they hit new PRs or volume?).
        2. Fatigue management (are they doing too many failure sets?).
        3. One actionable tip for their next session based on this data.
        Keep responses under 4 sentences. Be encouraging but scientific.
        """.trimIndent()
    ).build()

    private val historyChatClient = builder.defaultSystem(
        """
        You are 'Pulse', an elite, data-driven strength and conditioning AI coach.
        You are looking at a history of the same workout routine performed over time.
        Compare the most recent session against the previous sessions.
        Focus on:
        1. Trends in Progressive Overload (are weights or reps increasing over time?).
        2. Stalls or regressions (where is the user stuck?).
        3. Give one highly specific, actionable coaching tip for their next session based on this historical trend.
        Format your response beautifully. Keep it concise, professional, and encouraging.
        """.trimIndent()
    ).build()

    fun analyzeWorkout(userId: UUID, workoutId: String): Flow<String> = flow {

        val workout = withContext(Dispatchers.IO) {
            hevyClient.getWorkout(userId, workoutId)
        }
        val formattedWorkout = workoutContextService.formatWorkoutForAi(workout)

        val prompt = """
            Please analyze the following workout:
            
            Title: ${workout.title}
            Duration: ${calculateDuration(workout)} minutes
            
            Exercises:
            $formattedWorkout
        """.trimIndent()

        emitAll(
            chatClient.prompt()
                .user(prompt)
                .stream()
                .content()
                .asFlow()
        )
    }


    fun analyzeRoutineHistory(userId: UUID, routineId: UUID): Flow<String> = flow {

        val routine = withContext(Dispatchers.IO) {
            routineRepository.findByIdOrNull(routineId) ?: throw IllegalArgumentException("Routine not found")
        }

        val historyResponse = withContext(Dispatchers.IO) {
            hevyClient.getWorkouts(userId, page = 1, pageSize = 10)
        }

        val matchedWorkouts = historyResponse.workouts
            .filter { it.title == routine.name }
            .take(5)

        if (matchedWorkouts.isEmpty()) {
            emit("I couldn’t find any recent workout history for ‘${routine.name}’. Log some sessions and check back!")
            return@flow
        }

        val prompt = buildString {
            appendLine("Please analyze the history for the routine: ‘${routine.name}’.\n")

            matchedWorkouts.reversed().forEachIndexed { index, workout ->
                appendLine("--- WORKOUT ${index + 1} (Date: ${workout.startTime}) ---")
                appendLine(workoutContextService.formatWorkoutForAi(workout))
                appendLine()
            }
        }

        emitAll(
            historyChatClient.prompt()
                .user(prompt)
                .stream()
                .content()
                .asFlow()
        )
    }

    private fun calculateDuration(workout: HevyWorkout): Long {
        val start = workout.startTime.epochSecond
        val end = workout.endTime.epochSecond
        return (end - start) / 60
    }

}