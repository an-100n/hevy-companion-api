package com.hevycompanion.api.aichat.service

import com.hevycompanion.api.hevy.client.HevyClient
import com.hevycompanion.api.hevy.dto.HevyWorkout
import com.hevycompanion.api.hevy.repository.ExerciseDictionaryRepository
import com.hevycompanion.api.routine.repository.RoutineRepository
import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

@Service
class HevyAnalysisService(
    builder: ChatClient.Builder,
    private val hevyClient: HevyClient,
    private val dictionaryRepository: ExerciseDictionaryRepository,
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

    fun analyzeWorkout(userId: UUID, workoutId: String): Flux<String> {
        val workout = hevyClient.getWorkout(userId, workoutId)
        val formattedWorkout = formatWorkoutForAi(workout)

        val prompt = """
            Please analyze the following workout:
            
            Title: ${workout.title}
            Duration: ${calculateDuration(workout)} minutes
            
            Exercises:
            $formattedWorkout
        """.trimIndent()

        return chatClient.prompt()
            .user(prompt)
            .stream()
            .content()
    }

    fun analyzeRoutineHistory(userId: UUID, routineId: UUID): Flux<String> {
        // 1. Get the routine name from local DB
        val routine = routineRepository.findById(routineId)
            .orElseThrow { IllegalArgumentException("Routine not found") }
        
        // 2. Fetch a large batch of recent workouts from Hevy
        // We fetch 30 to ensure we get enough matches for this specific routine
        val historyResponse = hevyClient.getWorkouts(userId, page = 1, pageSize = 30)

        // 3. Filter to only include workouts matching this routine's name
        val matchedWorkouts = historyResponse.workouts
            .filter { it.title == routine.name }
            .take(5) // Take up to the 5 most recent

        if (matchedWorkouts.isEmpty()) {
            return Flux.just("I couldn't find any recent workout history for '${routine.name}'. Log some sessions and check back!")
        }

        // 4. Format the historical data
        val promptBuilder = StringBuilder()
        promptBuilder.append("Please analyze the history for the routine: '${routine.name}'.\n\n")
        
        // They arrive newest first, so we reverse them to show chronological progression to the AI (oldest to newest)
        matchedWorkouts.reversed().forEachIndexed { index, workout ->
            promptBuilder.append("--- WORKOUT ${index + 1} (Date: ${workout.startTime}) ---\n")
            promptBuilder.append(formatWorkoutForAi(workout))
            promptBuilder.append("\n")
        }

        // 5. Stream the historical analysis
        return historyChatClient.prompt()
            .user(promptBuilder.toString())
            .stream()
            .content()
    }

    private fun calculateDuration(workout: HevyWorkout): Long {
        val start = workout.startTime.epochSecond
        val end = workout.endTime.epochSecond
        return (end - start) / 60
    }

    private fun formatWorkoutForAi(workout: HevyWorkout): String {
        val builder = java.lang.StringBuilder()
        
        for (exercise in workout.exercises) {
            // Translate the Hex ID to a human-readable name using the local DB
            val exerciseName = dictionaryRepository.findById(exercise.exerciseTemplateId)
                .getOrNull()?.title ?: "Unknown Exercise (${exercise.exerciseTemplateId})"
            
            builder.append("- Exercise: $exerciseName\n")
            
            for (set in exercise.sets) {
                builder.append("  Set ${set.index}: ")
                builder.append("${set.reps ?: 0} reps @ ${set.weightKg ?: 0.0}kg ")
                if (set.type != "normal") builder.append("(${set.type}) ")
                if (set.rpe != null) builder.append("[RPE: ${set.rpe}] ")
                builder.append("\n")
            }
            builder.append("\n")
        }
        
        return builder.toString()
    }
}