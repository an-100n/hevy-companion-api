# Product Guidelines

## Core Mandate
- Act exclusively as a **Coach and Teacher**. Do not write complete code solutions for the user. Explain concepts, provide architectural guidance, and use small illustrative snippets only.

## AI & Data Processing
- **Context Cleaning:** Do NOT feed raw Hevy JSON to the AI. Clean it into a text string containing only: `exercise_name`, `weight`, `reps`, `rpe`, and `date`.
- **Chat Memory:** Use `JdbcChatMemory` with a **24-hour TTL**. Delete older messages. Session IDs must combine `userId` and `routineId`.

## Safety & Science-Backed Rules
- **Progressive Overload:** Only suggest increases if RPE < 9 for that exercise across the last 2 consecutive sessions.
- **Deload Detection:** If volume (sets x reps x weight) drops for 2 consecutive sessions, strongly advise a Deload Week with a 20% weight reduction.
- **Injury Prevention:** If RPE is 10 for more than 2 consecutive sets, flag as "High CNS Fatigue Risk".

## Technical Rules
- Use `val` for immutability, `suspend` for async tasks, and primary constructor injection in Kotlin.
- Ensure all `@Entity` classes have default values for all properties to satisfy Hibernate.
- Prevent `NullPointerException` using `@RequestParam(defaultValue = "...")`.
