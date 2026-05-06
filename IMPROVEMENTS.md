# Code Improvement Suggestions

## 🐛 Actual Bugs

### 1. `Routine.hevyRoutineId` is the wrong type — crash risk

`Routine.hevyRoutineId` is stored as `UUID`, but the Hevy API returns routine IDs as plain `String` (see `HevyRoutine.id: String`). In `RoutineService`, `UUID.fromString(selection.hevyRoutineId)` is called — if Hevy ever returns a non-UUID-format string ID, this crashes at runtime.

**Fix:** Change `hevyRoutineId` in `Routine.kt` from `UUID` to `String`. Update `RoutineRepository`, `RoutineService`, and `RoutineDtos` accordingly.

---

### 2. N+1 database query in `WorkoutContextService`

```kotlin
// Fires one SQL SELECT per exercise — a workout with 8 exercises = 8 DB round trips
for (exercise in workout.exercises) {
    val exerciseName = dictionaryRepository.findById(exercise.exerciseTemplateId)...
}
```

**Fix:** Batch-load all exercise IDs at once before the loop, then do an in-memory lookup:

```kotlin
val ids = workout.exercises.map { it.exerciseTemplateId }
val nameMap = dictionaryRepository.findAllById(ids).associate { it.id to it.title }

for (exercise in workout.exercises) {
    val exerciseName = nameMap[exercise.exerciseTemplateId] ?: "Unknown"
}
```

---

### 3. Dead DTOs with no controller

`RegisterRequest`, `LoginRequest`, and `AuthResponse` in `UserDtos.kt` are never used anywhere. The `/api/v1/auth/**` path is permitted in `SecurityConfig` but no controller handles it. `UserRepository.findByEmail` and `existsByEmail` are also never called. These are leftovers from a planned email/password flow.

**Fix:** Delete `RegisterRequest`, `LoginRequest`, `AuthResponse` from `UserDtos.kt`. Delete `findByEmail` and `existsByEmail` from `UserRepository`.

---

## ⚠️ Design Issues

### 4. `UUID.fromString(authentication.name)` duplicated everywhere

This pattern appears in every single controller method (8 times total).

**Fix:** Add a shared extension function, e.g. in a `extensions.kt` file:

```kotlin
fun Authentication.userId(): UUID = UUID.fromString(name)
```

Then replace all occurrences with `authentication.userId()`.

---

### 5. `HevyClient` hits the database on every API call

`getDecryptedApiKey(userId)` does a full DB read + AES decrypt on every method call. A single feature request can trigger multiple Hevy calls, each paying this cost.

**Fix:** Either accept the decrypted API key as a parameter (makes `HevyClient` a pure HTTP client), or fetch and decrypt once in the calling service and pass it down.

---

### 6. Redundant ID generation on `Routine`

```kotlin
@GeneratedValue(strategy = GenerationType.UUID)  // JPA generates the UUID...
val id: UUID = UUID.randomUUID()                 // ...and Kotlin also generates one. JPA always wins.
```

**Fix:** Remove `@GeneratedValue` and keep the Kotlin default, since you're already providing a default value. Or remove the default and let `@GeneratedValue` handle it exclusively. Pick one.

---

### 7. `username: String?` contradicts `nullable = false` on the DB column

```kotlin
@Column(name = "hevy_username", nullable = false, unique = true)
val username: String?,  // nullable in Kotlin, non-null in DB — they contradict each other
```

**Fix:** Change `username` to `String` (non-nullable) in the entity, since the column is `NOT NULL`. If it truly can be null, change the JPA annotation to `nullable = true`.

---

### 8. `spring.jpa.hibernate.ddl-auto=update` is dangerous in production

`update` silently alters your live database schema on startup. This can cause data loss or unexpected migrations in production.

**Fix:** Use Spring profiles. Keep `update` (or `create-drop`) in `application-dev.properties` and set `validate` (or `none`) in `application-prod.properties`. Add `spring.profiles.active=dev` locally.

---

## 💡 Modern / Best Practice Improvements

### 9. Remove 20+ `@JsonProperty` annotations with a global Jackson config

All `@JsonProperty` annotations on Hevy DTOs exist purely to map `snake_case` JSON → `camelCase` Kotlin. A single config replaces all of them:

```properties
# application.properties
spring.jackson.property-naming-strategy=SNAKE_CASE
spring.jackson.deserialization.fail-on-unknown-properties=false
```

Then delete every `@JsonProperty` and `@JsonIgnoreProperties` from `HevyDtos.kt`.

---

### 10. Hevy base URL is hardcoded in `HevyClient`

```kotlin
private val restClient = RestClient.builder()
    .baseUrl("https://api.hevyapp.com/v1")  // hardcoded — can't be overridden in tests
    .build()
```

**Fix:** Move to `application.properties`:

```properties
hevy.api.base-url=https://api.hevyapp.com/v1
```

Inject with `@Value("\${hevy.api.base-url}")` in `HevyClient`.

---

### 11. `syncProfile` saves a placeholder email instead of the real one

The Supabase JWT contains the user's email as a claim. Currently a `"placeholder@hevycompanion.com"` is saved and never updated.

**Fix:** Extract the email from the JWT in `UserController` and pass it to `syncProfile`:

```kotlin
// UserController
val jwt = authentication.principal as Jwt
val email = jwt.getClaimAsString("email") ?: throw IllegalStateException("No email in JWT")
userService.syncProfile(userId, email)
```

---

## ✅ To-Do List

- [ ] **Bug #1** — Change `Routine.hevyRoutineId` from `UUID` to `String`
- [ ] **Bug #2** — Fix N+1 query in `WorkoutContextService` (batch load with `findAllById`)
- [ ] **Bug #3** — Delete dead auth DTOs (`RegisterRequest`, `LoginRequest`, `AuthResponse`) and unused repository methods
- [ ] **Design #4** — Add `Authentication.userId()` extension function, replace all `UUID.fromString(authentication.name)` calls
- [ ] **Design #5** — Refactor `HevyClient` to not fetch the API key internally on every call
- [ ] **Design #6** — Remove redundant `@GeneratedValue` or the Kotlin default on `Routine.id`
- [ ] **Design #7** — Fix `User.username` nullability mismatch between Kotlin type and JPA column
- [ ] **Design #8** — Separate dev/prod Spring profiles, move `ddl-auto=update` out of the default config
- [ ] **Best Practice #9** — Add global Jackson `SNAKE_CASE` config, delete all `@JsonProperty` annotations
- [ ] **Best Practice #10** — Move Hevy base URL to `application.properties`, inject via `@Value`
- [ ] **Best Practice #11** — Extract real email from JWT in `syncProfile` instead of using placeholder
