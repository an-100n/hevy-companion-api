# 🛠 HevyPulse API: Detailed Endpoint & Feature Specification

This API acts as the bridge between Hevy's raw workout data and the Google Gemini Intelligence layer. It is built for 2026 standards: **High concurrency (Coroutines)**, **Stateless Auth (JWT)**, and **Cloud-Native persistence (Supabase)**.

## 🔑 1. Authentication & User Management (`/api/v1/auth`)
*The entry point for all PWA users.*

| Endpoint | Method | Security | Description |
| :--- | :--- | :--- | :--- |
| `/register` | `POST` | `permitAll` | Creates a new user in Supabase and initializes their profile. |
| `/login` | `POST` | `permitAll` | Validates credentials and returns a Bearer JWT. |
| `/profile` | `GET` | `JWT Required` | Returns the current user's physical stats (height, weight, sex) for AI grounding. |
| `/hevy-key` | `PUT` | `JWT Required` | Securely stores/updates the user's personal Hevy API Key (Encrypted at rest). |

## 📡 2. Hevy Integration & Webhooks (`/api/webhook`)
*Handles the automated "push" from Hevy's servers.*

| Endpoint | Method | Security | Description |
| :--- | :--- | :--- | :--- |
| `/workout` | `POST` | **Webhook Secret** | **Trigger:** Hevy sends `workoutId`. **Action:** Backend fetches workout, calls Gemini, and saves the "Pulse" summary. |
| `/verify` | `GET` | `permitAll` | Simple health check for the Hevy dashboard to verify the webhook URL is active. |

## 🤖 3. AI Chat & Coaching Engine (`/api/v1/chat`)
*Interactive AI interface powered by Gemini 3 Flash.*

| Endpoint | Method | Security | Description |
| :--- | :--- | :--- | :--- |
| `/` | `GET` | `JWT Required` | **Query Param:** `?message=...` Generic strength training advice from the AI coach. |
| `/analyze/{workoutId}` | `GET` | `JWT Required` | Generates a deep-dive analysis of a specific historical workout. |
| `/stream` | `GET` | `JWT Required` | **Streaming:** Uses Server-Sent Events (SSE) to stream Gemini's response for a better UX. |

## 📈 4. Workout Pulse & Analytics (`/api/v1/workouts`)
*The data users see in their PWA Feed.*

| Endpoint | Method | Security | Description |
| :--- | :--- | :--- | :--- |
| `/feed` | `GET` | `JWT Required` | Returns a list of recent workouts with their AI-generated "Next Time" tips. |
| `/stats` | `GET` | `JWT Required` | Aggregated data for the React charts (volume trends, PRs found by AI). |

---

## 🛠 Technical Implementation Details

### A. Environment-Level Security
* **Secrets Management**: No keys are stored in the repo. They are loaded via `~/.config/fish/conf.d/secrets.fish` (development) or `environment.d` (production).
* **Webhook Auth**: Uses a custom `Authorization: Bearer <WEBHOOK_TOKEN>` check to prevent "Replay Attacks" from non-Hevy sources.

### B. High-Performance Architecture
* **Non-Blocking IO**: All AI and Database calls are wrapped in Kotlin **Coroutines**, allowing the server to handle thousands of concurrent webhooks on minimal hardware (perfect for CachyOS efficiency).
* **Connection Pooling**: Uses **HikariCP** connected to the **Supabase Transaction Pooler (Port 6543)** to avoid IPv4/IPv6 handshake overhead.

### C. Developer Tooling
* **Custom Logging**: Minimalist, color-coded console output to prevent horizontal scrolling and focus on app-specific logic.
* **Liquibase/Flyway Support**: Prepared for database migrations to keep Supabase and Local DB schemas in sync.