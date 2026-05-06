package com.hevycompanion.api.aichat.service

import com.hevycompanion.api.hevy.dto.HevyWorkout
import com.hevycompanion.api.hevy.service.ExerciseDictionaryCache
import org.springframework.stereotype.Service
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Service
class WorkoutContextService(private val exerciseCache: ExerciseDictionaryCache) {

    private val dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

    fun formatWorkoutForAi(workout: HevyWorkout): String = buildString {
        appendLine("Date: ${workout.startTime.atZone(ZoneOffset.UTC).format(dateFormatter)}")

        for (exercise in workout.exercises) {
            appendLine("Exercise: ${exerciseCache.getTitle(exercise.exerciseTemplateId)}")

            exercise.sets.groupBy { it.type }.forEach { (type, sets) ->
                val setString = sets.joinToString(", ") { "${it.weightKg ?: 0.0}kg x ${it.reps ?: 0}" }
                appendLine("- ${type.replaceFirstChar { it.uppercase() }}: $setString")
            }
        }
    }
}
