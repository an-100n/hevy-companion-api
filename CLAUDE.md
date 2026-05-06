# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

HevyPulse API — an AI-powered companion and analytics engine for Hevy workout data. It bridges Hevy's fitness tracking API with Google Gemini AI to deliver real-time workout coaching via SSE streaming.

## Commands

```bash
# Build
./gradlew build

# Run
./gradlew bootRun

# Test (all)
./gradlew test

# Test (single test method)
./gradlew test --tests "com.hevycompanion.api.HevyCompanionApiApplicationTests.contextLoads"

# Test (single class)
./gradlew test --tests "com.hevycompanion.api.*"

# Package JAR
./gradlew bootJar
```

## Architecture

### Core Domains

**User & Auth** (`user/`)
Handles registration, profile management, and encrypted Hevy API key storage (AES-256-GCM). JWT validation is delegated entirely to Supabase — the app is an OAuth2 resource server that validates tokens against `https://vgdkmgetpzudngknevhc.supabase.co/auth/v1`.

**Hevy Proxy** (`hevy/`)
Proxies requests to `https://api.hevyapp.com/v1` on behalf of the user. Decrypts the stored Hevy API key per-request and forwards it. Also syncs an `ExerciseDictionary` (exercise templates) to give the AI grounding context.

**AI Chat** (`aichat/`)
Streaming chat via SSE using Kotlin Coroutines + Spring AI (Google Gemini). `HevyAnalysisService` assembles workout or routine context, then `ChatController` streams Gemini responses as `text/event-stream`. The reactive `Flux<String>` from Spring AI is converted to a Kotlin `Flow<String>`.

**Routine Tracking** (`routine/`)
Stores user routines linked to Hevy routine IDs, tracking sync status and activity history used for trend analysis.

### Request Flow

```
JWT (Supabase) → SecurityConfig (OAuth2 Resource Server)
                      ↓
UserController / HevyProxyController / ChatController
                      ↓
             Service layer (coroutine-dispatched)
                      ↓
     HevyClient (RestClient) ←→ Hevy API
     ChatClient (Spring AI)  ←→ Google Gemini
     JPA Repositories        ←→ PostgreSQL (Supabase)
```

### Concurrency Model

- All blocking DB and HTTP calls are dispatched on `Dispatchers.IO`
- SSE streaming endpoints return `Flow<ServerSentEvent<String>>`
- Spring AI's `Flux<String>` is converted to Flow via `.asFlow()`

### Security

- Stateless sessions (`SessionCreationPolicy.STATELESS`), no CSRF
- Public: `/api/v1/auth/**`, Swagger docs
- All other endpoints require valid Supabase JWT
- Hevy API keys encrypted at rest with AES-256-GCM; encryption secret loaded from `${HEVY_SECRETS_PATH}`

## Key Configuration

| Property | Purpose |
|---|---|
| `HEVY_SECRETS_PATH` | Path to secrets file containing `HEVY_ENCRYPTION_SECRET` |
| `AI_MODEL` | Google Gemini model name |
| Database URL/credentials | PostgreSQL connection via Supabase |

CORS is configured for `localhost:5173`, `127.0.0.1:5173`, `localhost:8081`, and `192.168.178.53:5173`.

## Tech Stack

- **Language:** Kotlin 2.x
- **Framework:** Spring Boot 4.x
- **Java:** 21
- **Database:** PostgreSQL via Supabase (HikariCP)
- **AI:** Google Gemini via Spring AI 2.0.0-M2
- **Auth:** Supabase JWT (OAuth2 Resource Server)
- **Concurrency:** Kotlin Coroutines
