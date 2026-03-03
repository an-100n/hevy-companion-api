package com.hevycompanion.api.hevy.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

@JsonIgnoreProperties(ignoreUnknown = true)
data class HevyRoutinesResponse(
    @JsonProperty("page_count")
    val pageCount: Int,
    @JsonProperty("page")
    val page: Int,
    @JsonProperty("routines")
    val routines: List<HevyRoutine>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class HevyRoutine(
    @JsonProperty("id")
    val id: String,
    @JsonProperty("title")
    val title: String,
    @JsonProperty("updated_at")
    val updatedAt: Instant,
    @JsonProperty("folder_id")
    val folderId: Int?,
    @JsonProperty("notes")
    val notes: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class HevyExerciseTemplatesResponse(
    @JsonProperty("page_count")
    val pageCount: Int,
    @JsonProperty("page")
    val page: Int,
    @JsonProperty("exercise_templates")
    val exerciseTemplates: List<HevyExerciseTemplate>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class HevyExerciseTemplate(
    @JsonProperty("id")
    val id: String,
    @JsonProperty("title")
    val title: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class HevyWorkoutsResponse(
    @JsonProperty("page_count")
    val pageCount: Int,
    @JsonProperty("page")
    val page: Int,
    @JsonProperty("workouts")
    val workouts: List<HevyWorkout>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class HevyWorkout(
    @JsonProperty("id")
    val id: String,
    @JsonProperty("title")
    val title: String,
    @JsonProperty("description")
    val description: String?,
    @JsonProperty("start_time")
    val startTime: Instant,
    @JsonProperty("end_time")
    val endTime: Instant,
    @JsonProperty("is_private")
    val isPrivate: Boolean?,
    @JsonProperty("exercises")
    val exercises: List<HevyExercise>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class HevyExercise(
    @JsonProperty("exercise_template_id")
    val exerciseTemplateId: String,
    @JsonProperty("superset_id")
    val supersetId: Int?,
    @JsonProperty("notes")
    val notes: String?,
    @JsonProperty("sets")
    val sets: List<HevySet>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class HevySet(
    @JsonProperty("index")
    val index: Int,
    @JsonProperty("type")
    val type: String, // e.g., "normal", "warmup", "failure", "dropset"
    @JsonProperty("weight_kg")
    val weightKg: Double?,
    @JsonProperty("reps")
    val reps: Int?,
    @JsonProperty("distance_meters")
    val distanceMers: Double?,
    @JsonProperty("duration_seconds")
    val durationSeconds: Int?,
    @JsonProperty("rpe")
    val rpe: Double?
)