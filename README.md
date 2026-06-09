# Hevy Companion API

A Kotlin / Spring Boot backend that bridges [Hevy](https://hevyapp.com) workout data with Gemini-powered coaching. It tracks strength trends and training load across sessions, catching stalled progress, flagging overreach, and suggesting rest or weight adjustments before the user notices the pattern themselves.

---

## Stack

| Layer | Technology |
|---|---|
| Runtime | Kotlin 2.2 · Java 21 |
| Framework | Spring Boot 4.0 (MVC, Security, JPA) |
| AI | Spring AI 2.0 · Google Gemini |
| Auth | Supabase JWT (OAuth2 Resource Server) |
| Database | PostgreSQL (Supabase, JPA/Hibernate) |
| Async | Kotlin coroutines · `Dispatchers.IO` |
| Streaming | Server-Sent Events via `Flow<ServerSentEvent>` |

---

## Getting Started

### Prerequisites

- JDK 21
- PostgreSQL (or a Supabase project)
- A Hevy account with an API key
- A Google AI API key (Gemini)

### Environment

Create `.env` in the project root:

```env
DATASOURCE_URL=jdbc:postgresql://<host>:6543/<db>?prepareThreshold=0
AI_API_KEY=<gemini-api-key>
AI_MODEL=gemini-2.5-flash-lite
HEVY_ENCRYPTION_SECRET=<64-char hex string>
HEVY_API_URL=https://api.hevyapp.com/v1
```

> `HEVY_ENCRYPTION_SECRET` must be a 256-bit (64 hex char) random key used for AES-256-GCM encryption of stored Hevy API keys.

### Run

```bash
./gradlew bootRun        # dev server on :8080
./gradlew build          # compile + test
./gradlew bootJar        # package JAR
```

---

## Architecture

```
Client (PWA)
  │  Supabase JWT (Bearer)
  ▼
Spring Boot :8080
  ├── SecurityConfig       OAuth2 resource server validates JWT against Supabase JWKS
  ├── UserController       profile sync, encrypted Hevy key storage
  ├── HevyProxyController  proxies Hevy API using per-user decrypted keys
  ├── RoutineController    persists and syncs active routines
  └── ChatController (SSE) streams Gemini coaching via Flow<ServerSentEvent>
          │
          ├── CoachingMetricsService   ACWR, e1RM, plateau & layoff detection
          ├── HevyAnalysisService      builds prompt, calls ChatClient, streams response
          └── WorkoutContextService    formats raw Hevy data for the AI prompt
```

**Hevy API keys** are never stored in plaintext. `EncryptionService` encrypts/decrypts with AES-256-GCM (random 12-byte IV prepended to ciphertext, Base64-encoded).

**Exercise names** are resolved via an in-memory `ExerciseDictionaryCache` loaded at startup — no N+1 DB queries per workout format call.

---

## API Reference

All endpoints require a valid Supabase JWT (`Authorization: Bearer <token>`).

### Users

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/users/sync` | Create or fetch local profile (call on sign-in) |
| `GET` | `/api/v1/users/profile` | Get profile + Hevy key status |
| `PUT` | `/api/v1/users/hevy-key` | Store (encrypted) Hevy API key |

### Hevy Proxy

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/hevy/workouts` | Paginated workout history (`?page=1&pageSize=10`) |
| `GET` | `/api/v1/hevy/workouts/{workoutId}` | Single workout detail |
| `GET` | `/api/v1/hevy/routines` | User's routines from Hevy |
| `POST` | `/api/v1/hevy/sync-dictionary` | Sync exercise name dictionary from Hevy |

### Routines

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/routines` | List active routines |
| `POST` | `/api/v1/routines/sync` | Select which Hevy routines to track |

### AI Coaching  *(Server-Sent Events — `Accept: text/event-stream`)*

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/chat/analyze/workout-history/{workoutId}` | Coaching review for a specific workout |
| `GET` | `/api/v1/chat/analyze/routine/{routineId}` | Coaching review across a routine's history |

The SSE stream emits plain text tokens. On error, a final `[Coaching Error: ...]` event is sent.

---

## Coaching Engine

Each AI review is grounded in pre-computed sports science metrics:

| Metric | Description |
|---|---|
| **ACWR** | Acute-to-Chronic Workload Ratio (requires 4+ sessions); flags overreach spikes or detraining dips |
| **e1RM** | Estimated one-rep max per exercise (`weight × (1 + reps/30)`) tracked across sessions |
| **Plateau detection** | Flags exercises where e1RM variance is < 1 % over 3+ sessions |
| **Training gap** | Detects return from layoff (≥ 7-day gap) and adjusts coaching tone |
| **Volume trend** | Total session volume history to contextualize load progression |
| **Failure sets** | Counts failure-type sets as a fatigue signal |

The AI prompt receives a structured brief of these metrics plus the raw formatted session data. The response is constrained to ~260 words, written as a voice-note-style post-session message — no jargon, no data echo, just interpretation and direction.

---

## Project Structure

```
src/main/kotlin/com/hevycompanion/api/
├── HevyCompanionApiApplication.kt
├── DotenvInit.kt                        # loads .env into Spring context
├── configuration/
│   ├── SecurityConfig.kt                # JWT auth, CORS
│   └── GlobalExceptionHandler.kt
├── user/
│   ├── entity/User.kt
│   ├── repository/UserRepository.kt
│   ├── service/
│   │   ├── UserService.kt
│   │   └── EncryptionService.kt         # AES-256-GCM
│   ├── controller/UserController.kt
│   └── dto/UserDtos.kt
├── hevy/
│   ├── entity/ExerciseDictionary.kt
│   ├── repository/ExerciseDictionaryRepository.kt
│   ├── client/HevyClient.kt             # RestClient wrapper
│   ├── service/
│   │   ├── HevySyncService.kt
│   │   └── ExerciseDictionaryCache.kt   # in-memory name lookup
│   ├── controller/HevyProxyController.kt
│   └── dto/HevyDtos.kt
├── routine/
│   ├── entity/Routine.kt
│   ├── repository/RoutineRepository.kt
│   ├── service/RoutineService.kt
│   ├── controller/RoutineController.kt
│   └── dto/RoutineDtos.kt
└── aichat/
    ├── service/
    │   ├── CoachingMetricsService.kt    # ACWR, e1RM, plateaus
    │   ├── WorkoutContextService.kt     # formats data for prompt
    │   └── HevyAnalysisService.kt       # ChatClient + streaming
    └── controller/ChatController.kt
```

---

## Security Notes

- The API is a **stateless OAuth2 resource server** — it never issues tokens, only validates them against Supabase's JWKS endpoint.
- Hevy API keys are encrypted with AES-256-GCM before every DB write and decrypted on read; plaintext never touches the database.
- CSRF is disabled (JWT-authenticated stateless API).
- CORS is locked to `localhost:5173`, `127.0.0.1:5173`, `localhost:8081`, and `192.168.*.*`.
