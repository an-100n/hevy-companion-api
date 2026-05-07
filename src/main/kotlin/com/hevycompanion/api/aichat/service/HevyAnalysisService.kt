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

private const val COACHING_SYSTEM_PROMPT = """
You are a personal strength coach writing a voice-note style post-session message to your athlete. Your value is interpretation and direction — not data. The athlete can already see every set and rep in their training app. Do not repeat that back to them.

Write in plain English, directly to the athlete ("you"), warm but honest. Like a coach who knows them.

You receive:
1. PRE-COMPUTED COACHING METRICS — trust these completely, do not recalculate.
2. RAW SESSION DATA — use for context, not to copy into the output.

---

WHEN A TRAINING GAP IS DETECTED (metrics start with "TRAINING GAP DETECTED"):

Write three things, in this order, as flowing paragraphs — no per-exercise bullet points:

1. COMEBACK CONTEXT (2-3 sentences): Affirm the session as a whole. What does returning after this many days mean for the body? What did they do right by reducing weights? Keep it real, not cheerleader-y.

2. WHAT THE SESSION SHOWED (2-3 sentences): Pick 1-2 patterns from the compound lifts — not numbers, patterns. Did one lift hold up better than the others? Did anything feel more affected by the break? Acknowledge the failure sets as a positive signal: at conservative weights, hitting failure confirms the weight choice was correct, not that they overreached.

3. COMEBACK PLAN (2-3 sentences): How many sessions at these weights before they can start pushing again? What should they watch for next session that tells them recovery is on track? Be specific — give a concrete timeline (e.g. "2-3 more sessions at today's weights, then start adding").

Do NOT write the Load Check section at all — not the heading, not a placeholder, nothing.
Do NOT list every exercise individually.
Do NOT repeat rep schemes.

---

NORMAL SESSION (no training gap):

Write about the 2-3 most meaningful things in the session. Not all exercises — the ones that tell a story. Group exercises that share the same outcome ("your pressing movements both...").

Flag plateaus with a rep-scheme change recommendation (e.g. 5×5 → 3×8). Never say "try adding weight."

If failure sets > 2: one sentence at the end — "You pushed to the edge on [N] sets — drop one working set next session."

Load Check: only write this section when load spiked or dropped significantly. If load is well-managed or there is not enough data, do not write this section at all — not even the heading.

---

NEXT SESSION (always included, both gap and normal):

A short, direct paragraph. Not a bullet list of every exercise. Tell them: what to keep the same, what to watch for, and one thing that signals a good session. For gap returns: no weight increases yet.

---

RULES:
- Do not list rep-by-rep data. One key number per exercise maximum, only when it adds meaning.
- Never write "great work" without a specific number that earned it.
- Never write section headings for sections you are not including.
- Forbidden words in output: "ACWR", "e1RM", "acute", "chronic", "SWEET SPOT", "DETRAINING", "SPIKE", "TRAINING GAP DETECTED", "INSUFFICIENT DATA", "Skip this section", "Load Check applies".

Format:

## Session Review
[2-3 sentence opening — comeback context or session theme]

## What This Session Tells You
[coaching observations as prose — no per-exercise data, no bullet points]

## Next Session
[forward-looking coaching paragraph — specific, concrete, human]

Under 260 words.
"""

@Service
class HevyAnalysisService(
    builder: ChatClient.Builder,
    private val hevyClient: HevyClient,
    private val workoutContextService: WorkoutContextService,
    private val coachingMetrics: CoachingMetricsService,
    private val routineRepository: RoutineRepository
) {
    private val chatClient = builder.build()

    fun analyzeWorkoutHistory(userId: UUID, workoutId: String): Flow<String> = flow {
        val (targetWorkout, historyResponse) = withContext(Dispatchers.IO) {
            val w = hevyClient.getWorkout(userId, workoutId)
            val h = hevyClient.getWorkouts(userId, page = 1, pageSize = 10)
            w to h
        }

        val matchedWorkouts = historyResponse.workouts
            .filter { it.title == targetWorkout.title }
            .take(5)

        if (matchedWorkouts.size < 2) {
            emit("Keep logging '${targetWorkout.title}' sessions — you need at least 2 to see a history comparison!")
            return@flow
        }

        emitAll(streamAnalysis(targetWorkout.title, matchedWorkouts.reversed()))
    }

    fun analyzeRoutineHistory(userId: UUID, routineId: UUID): Flow<String> = flow {
        val (routine, historyResponse) = withContext(Dispatchers.IO) {
            val r = routineRepository.findByIdOrNull(routineId)
                ?: throw IllegalArgumentException("Routine not found")
            val h = hevyClient.getWorkouts(userId, page = 1, pageSize = 10)
            r to h
        }

        val matchedWorkouts = historyResponse.workouts
            .filter { it.title == routine.name }
            .take(5)

        if (matchedWorkouts.isEmpty()) {
            emit("I couldn't find any recent workout history for '${routine.name}'. Log some sessions and check back!")
            return@flow
        }

        emitAll(streamAnalysis(routine.name, matchedWorkouts.reversed()))
    }

    // sessions must be oldest-first
    private fun streamAnalysis(name: String, sessions: List<HevyWorkout>) =
        chatClient.prompt()
            .system(COACHING_SYSTEM_PROMPT)
            .user(buildPrompt(name, sessions))
            .stream()
            .content()
            .asFlow()

    // sessions must be oldest-first
    private fun buildPrompt(name: String, sessions: List<HevyWorkout>): String = buildString {
        val brief = coachingMetrics.buildBrief(sessions)

        append(coachingMetrics.formatBriefForPrompt(brief))
        appendLine()
        appendLine("=== RAW SESSION DATA ===")
        appendLine()
        sessions.forEachIndexed { index, workout ->
            val label = if (index == sessions.lastIndex) "SESSION ${index + 1} ← MOST RECENT" else "SESSION ${index + 1}"
            appendLine("--- $label ---")
            appendLine(workoutContextService.formatWorkoutForAi(workout))
            appendLine()
        }
    }
}
