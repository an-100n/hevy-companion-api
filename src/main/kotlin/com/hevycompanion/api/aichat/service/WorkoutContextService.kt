package com.hevycompanion.api.aichat.service

import com.hevycompanion.api.hevy.dto.HevyWorkout
import com.hevycompanion.api.hevy.repository.ExerciseDictionaryRepository
import org.springframework.stereotype.Service
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.jvm.optionals.getOrNull

@Service
class WorkoutContextService(
    private val dictionaryRepository: ExerciseDictionaryRepository
) {

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy")
    }

    fun formatWorkoutForAi(workout: HevyWorkout): String = buildString {
        val date = workout.startTime.atZone(ZoneOffset.UTC).format(DATE_FORMATTER)
        appendLine("Date: $date")

        for (exercise in workout.exercises) {
            val exerciseName = dictionaryRepository.findById(exercise.exerciseTemplateId)
                .getOrNull()?.title ?: "Unknown"
            appendLine("Exercise: $exerciseName")

            exercise.sets.groupBy { it.type }.forEach { (type, sets) ->
                val setString = sets.joinToString(", ") { set ->
                    "${set.weightKg ?: 0.0}kg x ${set.reps ?: 0}"
                }
                appendLine("- ${type.replaceFirstChar { it.uppercase() }}: $setString")
            }
        }
    }
}
