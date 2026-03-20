package com.hevycompanion.api.aichat.service

import com.hevycompanion.api.hevy.dto.HevyWorkout
import com.hevycompanion.api.hevy.repository.ExerciseDictionaryRepository
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter
import kotlin.jvm.optionals.getOrNull

@Service
class WorkoutContextService(
    private val dictionaryRepository: ExerciseDictionaryRepository
) {

    fun formatWorkoutForAi(workout: HevyWorkout): String {
        val builder = StringBuilder()

        val date = workout.startTime.atZone(java.time.ZoneId.of("UTC"))
            .format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
        builder.append("Date: $date\n")

        for (exercise in workout.exercises) {
            val exerciseName =
                dictionaryRepository.findById(exercise.exerciseTemplateId).getOrNull()?.title ?: "Unknown"
            builder.append("Exercise: $exerciseName\n")

            val groupedSets = exercise.sets.groupBy { it.type }

            for ((type, sets) in groupedSets) {
                // e.g. "warmup", "normal"
                val setString = sets.joinToString(", ") { set ->
                    // Build string like "33kg x 10"
                    val weight = set.weightKg ?: 0.0
                    val reps = set.reps ?: 0
                    "${weight}kg x $reps"
                }

                builder.append("- ${type.replaceFirstChar { it.uppercase() }}: $setString\n")
            }

        }
        return builder.toString()

    }
}