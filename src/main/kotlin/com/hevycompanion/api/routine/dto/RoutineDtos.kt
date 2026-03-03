package com.hevycompanion.api.routine.dto

data class RoutineSelection(
    val hevyRoutineId: String,
    val name: String
)

data class SyncRoutinesRequest(
    val selectedRoutines: List<RoutineSelection>
)

data class ActiveRoutineResponse(
    val id: String,
    val hevyRoutineId: String,
    val name: String
)