package com.hevycompanion.api.aichat.service

import com.hevycompanion.api.hevy.dto.HevySet
import com.hevycompanion.api.hevy.dto.HevyWorkout
import com.hevycompanion.api.hevy.service.ExerciseDictionaryCache
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class CoachingMetricsService(private val exerciseCache: ExerciseDictionaryCache) {

    enum class AcwrZone { SWEET_SPOT, SPIKE, DETRAINING, INSUFFICIENT_DATA }

    data class ExerciseTrend(
        val name: String,
        val e1rmHistory: List<Double>,  // oldest → newest, sessions where the exercise was performed
        val isPlateaued: Boolean,
        val trendSymbol: String,        // ↑ → ↓
        val failureSetCount: Int
    )

    data class CoachingBrief(
        val sessionCount: Int,
        val daysSinceLastSession: Long?,   // null if only one session
        val isReturnFromLayoff: Boolean,   // true when gap >= 7 days
        val acwr: Double?,
        val acwrZone: AcwrZone,
        val volumeHistory: List<Double>,
        val exerciseTrends: List<ExerciseTrend>,
        val totalFailureSetsLatest: Int,
        val hasRpeData: Boolean
    )

    // sessions must be passed oldest-first
    fun buildBrief(sessions: List<HevyWorkout>): CoachingBrief {
        val volumes = sessions.map { sessionVolume(it) }
        val acwr = calculateAcwr(volumes)
        val acwrZone = when {
            acwr == null -> AcwrZone.INSUFFICIENT_DATA
            acwr > 1.5   -> AcwrZone.SPIKE
            acwr < 0.8   -> AcwrZone.DETRAINING
            else         -> AcwrZone.SWEET_SPOT
        }

        val daysSinceLastSession = if (sessions.size >= 2) {
            Duration.between(sessions[sessions.lastIndex - 1].startTime, sessions.last().startTime).toDays()
        } else null

        val latestSession = sessions.last()
        val totalFailureSets = latestSession.exercises
            .flatMap { it.sets }
            .count { it.type == "failure" }

        val hasRpeData = sessions.any { w -> w.exercises.any { e -> e.sets.any { s -> s.rpe != null } } }

        return CoachingBrief(
            sessionCount = sessions.size,
            daysSinceLastSession = daysSinceLastSession,
            isReturnFromLayoff = daysSinceLastSession != null && daysSinceLastSession >= 7,
            acwr = acwr,
            acwrZone = acwrZone,
            volumeHistory = volumes,
            exerciseTrends = buildExerciseTrends(sessions),
            totalFailureSetsLatest = totalFailureSets,
            hasRpeData = hasRpeData
        )
    }

    fun formatBriefForPrompt(brief: CoachingBrief): String = buildString {
        appendLine("=== PRE-COMPUTED COACHING METRICS (trust these values) ===")
        appendLine()

        // Training gap — placed first so the model reads this context before anything else
        if (brief.isReturnFromLayoff) {
            appendLine("⚠ TRAINING GAP DETECTED: ${brief.daysSinceLastSession} days since last session")
            appendLine("CONTEXT: This is a return-from-layoff session.")
            appendLine("Weight reductions in this session are INTENTIONAL and the correct response to deconditioning.")
            appendLine("Do NOT treat lower weights as regressions. Do NOT flag them as problems.")
            appendLine("Next session targets should STAY at today's weights. Do not push for increases yet.")
        } else if (brief.daysSinceLastSession != null) {
            appendLine("Days since last session: ${brief.daysSinceLastSession}")
        }
        appendLine()

        // ACWR
        val acwrLine = when (brief.acwrZone) {
            AcwrZone.SWEET_SPOT        -> "Overall training load: ${"%.2f".format(brief.acwr)} — well-managed ✓"
            AcwrZone.SPIKE             -> "Overall training load: ${"%.2f".format(brief.acwr)} — jumped too fast ⚠ (threshold >1.5)"
            AcwrZone.DETRAINING        -> "Overall training load: ${"%.2f".format(brief.acwr)} — too low ↓ (threshold <0.8)"
            AcwrZone.INSUFFICIENT_DATA -> "Overall training load: not enough sessions to calculate (need 4+)"
        }
        appendLine(acwrLine)

        val volumeStr = brief.volumeHistory.joinToString(" → ") { "${it.toInt()} kg" }
        appendLine("Session volumes (oldest → newest): $volumeStr")
        appendLine()

        if (brief.totalFailureSetsLatest > 0) {
            val flag = if (brief.totalFailureSetsLatest > 2) " ⚠ HIGH" else ""
            appendLine("Sets taken to failure in latest session: ${brief.totalFailureSetsLatest}$flag")
            appendLine()
        }

        appendLine("Strength trends per exercise (estimated max strength, normal sets only):")
        for (ex in brief.exerciseTrends) {
            val historyStr = ex.e1rmHistory.joinToString(" → ") { "${it.toInt()} kg" }
            val plateauFlag = if (ex.isPlateaued) "  ⚠ PLATEAU (no meaningful change in 3+ sessions)" else ""
            appendLine("  ${ex.trendSymbol} ${ex.name}: $historyStr$plateauFlag")
        }
        appendLine()

        if (brief.hasRpeData) {
            appendLine("RPE data: available — see raw session logs below")
        } else {
            appendLine("RPE data: not recorded by athlete")
        }
    }

    // --- Private calculation helpers ---

    private fun sessionVolume(workout: HevyWorkout): Double =
        workout.exercises.flatMap { it.sets }
            .filter { it.type == "normal" }
            .sumOf { (it.weightKg ?: 0.0) * (it.reps ?: 0) }

    private fun bestE1rm(sets: List<HevySet>): Double =
        sets.filter { it.type == "normal" }
            .mapNotNull { set ->
                val weight = set.weightKg ?: return@mapNotNull null
                val reps = set.reps?.takeIf { it > 0 } ?: return@mapNotNull null
                weight * (1.0 + reps / 30.0)
            }
            .maxOrNull() ?: 0.0

    // Requires at least 4 sessions: 1 acute + 3 chronic. Returns null otherwise.
    private fun calculateAcwr(volumes: List<Double>): Double? {
        if (volumes.size < 4) return null
        val acute = volumes.last()
        val chronic = volumes.dropLast(1).average()
        return if (chronic == 0.0) null else acute / chronic
    }

    // Plateau = max–min variance less than 1% of the average, across 3+ sessions.
    private fun isPlateaued(values: List<Double>): Boolean {
        if (values.size < 3) return false
        val avg = values.average()
        if (avg == 0.0) return false
        val range = values.maxOrNull()!! - values.minOrNull()!!
        return (range / avg) < 0.01
    }

    private fun trendSymbol(values: List<Double>): String {
        if (values.size < 2) return "→"
        val change = if (values.first() == 0.0) 0.0 else (values.last() - values.first()) / values.first()
        return when {
            change > 0.01  -> "↑"
            change < -0.01 -> "↓"
            else           -> "→"
        }
    }

    private fun buildExerciseTrends(sessions: List<HevyWorkout>): List<ExerciseTrend> {
        // Only analyse exercises that appear in the most recent session.
        val latestExerciseIds = sessions.last().exercises.map { it.exerciseTemplateId }.distinct()

        return latestExerciseIds.mapNotNull { templateId ->
            val name = exerciseCache.getTitle(templateId)
            if (name == "Unknown") return@mapNotNull null

            // One e1RM entry per session where this exercise was actually performed.
            val e1rmHistory = sessions.mapNotNull { workout ->
                val exercise = workout.exercises.find { it.exerciseTemplateId == templateId }
                    ?: return@mapNotNull null
                val e1rm = bestE1rm(exercise.sets)
                if (e1rm > 0.0) e1rm else null
            }

            if (e1rmHistory.isEmpty()) return@mapNotNull null

            val failureCount = sessions.last().exercises
                .find { it.exerciseTemplateId == templateId }
                ?.sets?.count { it.type == "failure" } ?: 0

            ExerciseTrend(
                name = name,
                e1rmHistory = e1rmHistory,
                isPlateaued = isPlateaued(e1rmHistory),
                trendSymbol = trendSymbol(e1rmHistory),
                failureSetCount = failureCount
            )
        }
    }
}
