# 🏋️‍♂️ AI-Powered Fitness Recommendation Platform

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen.svg?logo=springboot)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2025.x-blue.svg?logo=spring)](https://spring.io/projects/spring-cloud)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791.svg?logo=postgresql)](https://www.postgresql.org/)
[![MongoDB](https://img.shields.io/badge/MongoDB-8.0-47A248.svg?logo=mongodb)](https://www.mongodb.com/)
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3.13-FF6600.svg?logo=rabbitmq)](https://www.rabbitmq.com/)
[![Keycloak](https://img.shields.io/badge/Keycloak-26.7-informational.svg?logo=keycloak)](https://www.keycloak.org/)
[![Google Gemini](https://img.shields.io/badge/AI-Google%20Gemini-4285F4.svg?logo=google)](https://ai.google.dev/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED.svg?logo=docker)](https://www.docker.com/)

An enterprise-grade, event-driven microservices ecosystem for fitness tracking and AI-powered workout intelligence. Users log workout activities, and an asynchronous pipeline leverages **Google Gemini AI** to generate personalized performance analyses, recovery suggestions, training progressions, and safety recommendations in real time.

---

## 📑 Table of Contents

- [System Architecture](#-system-architecture)
- [End-to-End Workflow](#-end-to-end-workflow)
- [Microservices Catalog](#-microservices-catalog)
- [Infrastructure & Supporting Services](#-infrastructure--supporting-services)
- [Comprehensive REST API Reference](#-comprehensive-rest-api-reference)
  - [API Gateway (`8080`)](#1-api-gateway-routes-port-8080)
  - [User Service (`8081`)](#2-user-service-port-8081)
  - [Activity Service (`8082`)](#3-activity-service-port-8082)
  - [AI Recommendation Service (`8083`)](#4-ai-recommendation-service-port-8083)
- [Step-by-Step Testing Walkthrough](#-step-by-step-testing-walkthrough)
- [Getting Started & Local Setup](#-getting-started--local-setup)
  - [Prerequisites](#prerequisites)
  - [1. Environment Configuration](#1-environment-configuration)
  - [2. Run Option A: Hybrid Development (Recommended)](#2-run-option-a-hybrid-development-recommended)
  - [3. Run Option B: Full Stack in Docker](#3-run-option-b-full-stack-in-docker)
  - [4. Run Option C: Production Deployment](#4-run-option-c-production-deployment)
- [Spring Profiles & Configuration Hierarchy](#-spring-profiles--configuration-hierarchy)
- [Event-Driven Messaging Topology](#-event-driven-messaging-topology)
- [Troubleshooting & FAQ](#-troubleshooting--faq)
- [Service Documentation Index](#-service-documentation-index)

---

## 🏛 System Architecture

```
                                    ┌────────────────────────────────┐
                                    │    Client App / Web / cURL     │
                                    └───────────────┬────────────────┘
                                                    │ Bearer JWT (OIDC)
                                                    ▼
┌───────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                       API GATEWAY (Port: 8080)                                        │
│  - Spring Cloud Gateway (WebFlux)                                                                     │
│  - OAuth2 Resource Server (JWT verification with Keycloak)                                            │
│  - KeycloakUserSyncFilter: Validates & auto-syncs user profiles, injects 'X-User-ID' downstream       │
└───────────────┬───────────────────────────────┬───────────────────────────────┬───────────────────────┘
                │                               │                               │
                │ /api/v1/users/**              │ /api/v1/activities/**         │ /api/v1/recommendations/**
                ▼                               ▼                               ▼
┌───────────────────────────────┐ ┌───────────────────────────┐ ┌───────────────────────────────────────┐
│         USER SERVICE          │ │     ACTIVITY SERVICE      │ │              AI SERVICE               │
│         (Port: 8081)          │ │       (Port: 8082)        │ │             (Port: 8083)              │
│                               │ │                           │ │                                       │
│ - User registration & profile │ │ - Workout logging & query │ │ - Async workout event consumer        │
│ - Keycloak UUID validation    │ │ - Validates user via      │ │ - Google Gemini AI Prompt Engine      │
│ - Database: PostgreSQL 16     │ │   WebClient to UserSvc    │ │ - Parses JSON recommendations         │
│                               │ │ - Database: MongoDB 8     │ │ - Database: MongoDB 8                 │
└───────────────┬───────────────┘ └─────────────┬─────────────┘ └───────────────────▲───────────────────┘
                │                               │                                   │
                │                               │ Publishes Activity Event          │ Consumes Activity Event
                │                               ▼                                   │
                │                     ┌─────────────────────────────────────────────┴┐
                │                     │             RABBITMQ BROKER                  │
                │                     │  Exchange: fitness.exchange (direct)         │
                │                     │  Routing Key: activity.tracking              │
                │                     │  Queue: activity.queue                       │
                │                     └──────────────────────────────────────────────┘
                │                                                                   │
                │                                                                   ▼
                │                                                     ┌──────────────────────────┐
                │                                                     │     GOOGLE GEMINI AI     │
                │                                                     │  LLM Generative API      │
                │                                                     └──────────────────────────┘
                ▼
┌───────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                CENTRAL INFRASTRUCTURE & DISCOVERY                                     │
│  - Eureka Discovery Server (Port: 8761)  ── Dynamic registry & client-side load balancing            │
│  - Spring Cloud Config Server (Port: 8888) ── Externalized multi-profile configuration repository      │
│  - Keycloak IAM (Port: 8180) ── OAuth2.0 / OpenID Connect Identity Provider                            │
└───────────────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 🔄 End-to-End Workflow

```
Client             Keycloak         Gateway (8080)       UserSvc (8081)    ActivitySvc (8082)    RabbitMQ        AISvc (8083)       Gemini AI
  │                   │                   │                    │                  │                 │              │                │
  │── 1. Auth Request─>│                   │                    │                  │                 │              │                │
  │<── Access JWT ────│                   │                    │                  │                 │              │                │
  │                                       │                    │                  │                 │              │                │
  │── 2. POST /api/v1/activities ────────>│                    │                  │                 │              │                │
  │      (Bearer JWT)                     │── 3. Validate ────>│                  │                 │              │                │
  │                                       │      & Auto-Sync   │                  │                 │              │                │
  │                                       │<── 200/201 User ───│                  │                 │              │                │
  │                                       │                                       │                 │              │                │
  │                                       │── 4. Forward with X-User-ID ─────────>│                 │              │                │
  │                                       │                                       │                 │              │                │
  │                                       │                                       │── 5. Validate ─>│              │                │
  │                                       │                                       │      User Svc   │              │                │
  │                                       │                                       │<── 200 OK ──────│              │                │
  │                                       │                                       │                 │              │                │
  │                                       │                                       │── 6. Save DB ──>│              │                │
  │                                       │                                       │── 7. Publish ──>│              │                │
  │<── 8. 200 OK (Activity JSON) ─────────│<──────────────────────────────────────│                  │              │                │
  │                                                                                                  │── 8. Consume>│                │
  │                                                                                                                 │── 9. Prompt ──>│
  │                                                                                                                 │<── AI JSON ────│
  │                                                                                                                 │── 10. Save DB ─│
  │                                                                                                                                  │
  │── 11. GET /api/v1/recommendations/activity/{id} ───────────────────────────────────────────────────────────────>│
  │<── 12. 200 OK (Structured AI Recommendation) ───────────────────────────────────────────────────────────────────│
```

---

## 📦 Microservices Catalog

| Microservice | Directory | Direct Port | Gateway Route | Database / Store | Responsibilities |
|---|---|---|---|---|---|
| **Eureka Server** | [`eurekaservice`](./eurekaservice) | `8761` | — | In-memory Registry | Service discovery, heartbeat monitoring, and instance resolution |
| **Config Server** | [`configserver`](./configserver) | `8888` | — | Classpath Native Repo | Centralized multi-environment configuration (`dev`, `docker`, `prod`) |
| **Gateway Service** | [`gatewayservice`](./gatewayservice) | `8080` | Entry Point | — | Reverse proxy, Keycloak JWT auth, user auto-sync, header injection |
| **User Service** | [`userservice`](./userservice) | `8081` | `/api/v1/users/**` | PostgreSQL 16 (`userdb`) | User profiles, role management, Keycloak ID existence validation |
| **Activity Service** | [`activityservice`](./activityservice) | `8082` | `/api/v1/activities/**` | MongoDB 8 (`activitydb`) | Workout tracking, metric persistence, RabbitMQ event publishing |
| **AI Recommendation** | [`aiservice`](./aiservice) | `8083` | `/api/v1/recommendations/**` | MongoDB 8 (`recommendationdb`) | Consumes workout events, queries Gemini AI, stores coaching insights |

---

## 🛠 Infrastructure & Supporting Services

| Component | Default Port | Internal Port | Description | Credentials (Dev) |
|---|---|---|---|---|
| **Keycloak IAM** | `8180` | `8080` | OpenID Connect identity provider (`fitness-oauth2` realm) | `admin` / `admin` |
| **PostgreSQL 16** | `5432` | `5432` | Relational DB for `userdb` and `keycloak` databases | `postgres` / `postgres` |
| **MongoDB 8** | `27017` | `27017` | Document DB for `activitydb` and `recommendationdb` | `mongo` / `mongo` |
| **RabbitMQ 3.13** | `5672` | `5672` | AMQP Message Broker | `rabbitmq` / `rabbitmq` |
| **RabbitMQ Admin UI** | `15672` | `15672` | Web dashboard for queues, exchanges, and message rates | `rabbitmq` / `rabbitmq` |

---

## 📖 Comprehensive REST API Reference

All application endpoints are accessible either directly on their individual service port (for internal/dev testing) or securely through the **API Gateway** on port `8080`.

---

### 1. API Gateway Routes (Port `8080`)

All requests through the Gateway (except `/actuator/*`) require an `Authorization: Bearer <JWT>` header from Keycloak.

| Method | Public Gateway Path | Downstream Destination | Header Injected |
|---|---|---|---|
| `*` | `/api/v1/users/**` | `lb://user-service` | `X-User-ID: <keycloak-sub>` |
| `*` | `/api/v1/activities/**` | `lb://activity-service` | `X-User-ID: <keycloak-sub>` |
| `*` | `/api/v1/recommendations/**` | `lb://ai-service` | `X-User-ID: <keycloak-sub>` |

---

### 2. User Service (Port `8081`)

#### `POST /api/v1/users/register`
Registers a new user profile or returns the existing profile if already registered.

- **Direct URL:** `http://localhost:8081/api/v1/users/register`
- **Gateway URL:** `http://localhost:8080/api/v1/users/register`
- **Request Body:**
```json
{
  "keycloakId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "email": "sarah.connor@example.com",
  "firstName": "Sarah",
  "lastName": "Connor"
}
```
- **Response (`201 Created` / `200 OK`):**
```json
{
  "id": "e7b0a708-59c4-4b51-9257-2384cf22485f",
  "keycloakId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "email": "sarah.connor@example.com",
  "firstName": "Sarah",
  "lastName": "Connor",
  "createdAt": "2026-08-29T08:00:00",
  "updatedAt": "2026-08-29T08:00:00"
}
```

#### `GET /api/v1/users/{userId}`
Retrieves a user profile by internal PostgreSQL UUID.

- **Direct URL:** `http://localhost:8081/api/v1/users/e7b0a708-59c4-4b51-9257-2384cf22485f`
- **Response (`200 OK`):** User object as above.
- **Response (`404 Not Found`):**
```json
{
  "status": 404,
  "error": "NOT_FOUND",
  "message": "User not found with id: e7b0a708-59c4-4b51-9257-2384cf22485f",
  "timestamp": "2026-08-29T08:05:00",
  "validationErrors": []
}
```

#### `GET /api/v1/users/{keycloakId}/validate`
Checks whether a user exists by their Keycloak UUID. Used by `activityservice` and `gatewayservice`.

- **Direct URL:** `http://localhost:8081/api/v1/users/3fa85f64-5717-4562-b3fc-2c963f66afa6/validate`
- **Response (`200 OK`):** `true` or `false`

---

### 3. Activity Service (Port `8082`)

Supported `ActivityType` values: `RUNNING`, `WALKING`, `CYCLING`, `SWIMMING`, `WEIGHT_TRAINING`, `YOGA`, `HIIT`, `CARDIO`, `STRETCHING`, `OTHER`.

#### `POST /api/v1/activities`
Tracks a new fitness activity, persists it to MongoDB, and publishes an event to RabbitMQ for AI analysis.

- **Direct URL:** `http://localhost:8082/api/v1/activities`
- **Gateway URL:** `http://localhost:8080/api/v1/activities`
- **Headers:**
  - `Content-Type: application/json`
  - `X-User-ID: 3fa85f64-5717-4562-b3fc-2c963f66afa6` *(Auto-injected by Gateway when using Bearer token)*
- **Request Body:**
```json
{
  "type": "RUNNING",
  "duration": 45,
  "caloriesBurn": 480,
  "startTime": "2026-08-29T07:15:00",
  "additionalMetrics": {
    "distanceKm": 8.2,
    "avgHeartRate": 158,
    "maxHeartRate": 176,
    "paceMinPerKm": 5.48,
    "elevationGainM": 85
  }
}
```
- **Response (`200 OK`):**
```json
{
  "id": "6d7293a7-e852-4fc8-9037-123456789abc",
  "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "type": "RUNNING",
  "duration": 45,
  "caloriesBurn": 480,
  "startTime": "2026-08-29T07:15:00",
  "additionalMetrics": {
    "distanceKm": 8.2,
    "avgHeartRate": 158,
    "maxHeartRate": 176,
    "paceMinPerKm": 5.48,
    "elevationGainM": 85
  },
  "createdAt": "2026-08-29T08:00:00",
  "updatedAt": "2026-08-29T08:00:00"
}
```

#### `GET /api/v1/activities`
Retrieves all recorded workout sessions for the current user.

- **Headers:** `X-User-ID: 3fa85f64-5717-4562-b3fc-2c963f66afa6`
- **Response (`200 OK`):** Array of Activity objects.

#### `GET /api/v1/activities/{activityId}`
Retrieves a single activity by its unique identifier.

- **Direct URL:** `http://localhost:8082/api/v1/activities/6d7293a7-e852-4fc8-9037-123456789abc`
- **Response (`200 OK`):** Single Activity object.

---

### 4. AI Recommendation Service (Port `8083`)

#### `GET /api/v1/recommendations/user/{userId}`
Retrieves all generated AI recommendations for a specific user ID.

- **Direct URL:** `http://localhost:8083/api/v1/recommendations/user/3fa85f64-5717-4562-b3fc-2c963f66afa6`
- **Gateway URL:** `http://localhost:8080/api/v1/recommendations/user/3fa85f64-5717-4562-b3fc-2c963f66afa6`
- **Response (`200 OK`):** Array of Recommendation objects.

#### `GET /api/v1/recommendations/activity/{activityId}`
Retrieves the AI recommendation generated for a specific workout session.

- **Direct URL:** `http://localhost:8083/api/v1/recommendations/activity/6d7293a7-e852-4fc8-9037-123456789abc`
- **Gateway URL:** `http://localhost:8080/api/v1/recommendations/activity/6d7293a7-e852-4fc8-9037-123456789abc`
- **Response (`200 OK`):**
```json
{
  "id": "b18a204f-3c87-4184-9092-23fba930d433",
  "activityId": "6d7293a7-e852-4fc8-9037-123456789abc",
  "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "activityType": "RUNNING",
  "recommendation": "Overall: Excellent aerobic threshold run with strong sustained output over 45 minutes.\n\nPace: Steady pace of 5.48 min/km indicates disciplined effort distribution.\n\nHeart Rate: Average heart rate of 158 bpm demonstrates solid aerobic endurance conditioning.\n\nCalories Burned: 480 kcal accurately reflects the high intensity and elevation gain.\n\n",
  "improvements": [
    "Hill Climbing: Maintain consistent stride cadence when hitting the 85m elevation incline.",
    "Pacing Strategy: Incorporate negative splits on the last 2 km."
  ],
  "suggestions": [
    "Recovery Run: 30 minutes light jog at 6:30 min/km in 48 hours.",
    "Strength Routine: Single-leg Bulgarian split squats and calf raises for running stability."
  ],
  "safety": [
    "Rehydrate with electrolyte fluids to replace sodium loss during high heart rate runs.",
    "Perform dedicated static hamstring and Achilles tendon stretches."
  ],
  "createdAt": "2026-08-29T08:00:05"
}
```

---

## 🚀 Step-by-Step Testing Walkthrough

Follow this complete walkthrough to test the entire ecosystem end-to-end:

### Step 1: Obtain a JWT Token from Keycloak
```bash
TOKEN=$(curl -s -X POST "http://localhost:8180/realms/fitness-oauth2/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=fitness-client" \
  -d "username=fitness_user" \
  -d "password=password123" | jq -r '.access_token')

echo "Access Token: $TOKEN"
```

### Step 2: Track a Workout via API Gateway
```bash
curl -i -X POST http://localhost:8080/api/v1/activities \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "RUNNING",
    "duration": 45,
    "caloriesBurn": 480,
    "startTime": "2026-08-29T07:15:00",
    "additionalMetrics": {
      "distanceKm": 8.2,
      "avgHeartRate": 158,
      "maxHeartRate": 176
    }
  }'
```
> **What happens automatically under the hood:**
> 1. The Gateway validates the JWT.
> 2. `KeycloakUserSyncFilter` registers the user in PostgreSQL `userdb` if not already present.
> 3. `activityservice` validates user existence and saves the activity in MongoDB `activitydb`.
> 4. `activityservice` publishes the event to RabbitMQ `activity.queue`.
> 5. `aiservice` consumes the event, calls Google Gemini AI, and saves the recommendation in MongoDB `recommendationdb`.

### Step 3: Fetch the Generated AI Recommendation
```bash
# Replace with the activityId returned from Step 2
ACTIVITY_ID="6d7293a7-e852-4fc8-9037-123456789abc"

curl -X GET "http://localhost:8080/api/v1/recommendations/activity/$ACTIVITY_ID" \
  -H "Authorization: Bearer $TOKEN" | jq
```

---

## 💻 Getting Started & Local Setup

### Prerequisites

- **Java 21 JDK** (e.g., Eclipse Temurin, OpenJDK, or Homebrew)
- **Maven 3.9+** (or use included `./mvnw` wrappers)
- **Docker Desktop** & **Docker Compose v2**
- **Google Gemini API Key** (from [Google AI Studio](https://aistudio.google.com/))

---

### 1. Environment Configuration

Create development and production environment files:
```bash
cp .env.example .env.dev
cp .env.example .env.prod
```

Edit `.env.dev` and add your **Google Gemini API key**:
```env
GEMINI_API_URL=https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent
GEMINI_API_KEY=AIzaSy...your_actual_key_here
```

Make database and RabbitMQ init scripts executable:
```bash
chmod +x docker/postgres/init/*.sh
chmod +x docker/mongodb/init/*.sh
chmod +x docker/rabbitmq/init/*.sh
```

---

### 2. Run Option A: Hybrid Development (Recommended)

Run **databases, message broker, and discovery infrastructure in Docker**, while running the Spring Boot microservices directly on your machine / IDE for fast debugging and hot-reloading.

#### Step 1: Start Infrastructure in Docker
```bash
docker compose --env-file .env.dev -f docker-compose.dev.yml up -d
```
*This spins up: PostgreSQL, MongoDB, RabbitMQ, Eureka Server, Config Server, Keycloak, and Gateway.*

#### Step 2: Start Microservices (Order Matters)
Open separate terminal tabs (or run from IntelliJ / VS Code) using profile `dev`:

```bash
# Terminal 1 — User Service (Port 8081)
cd userservice && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Terminal 2 — Activity Service (Port 8082)
cd activityservice && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Terminal 3 — AI Recommendation Service (Port 8083)
cd aiservice && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

#### Step 3: Verify Infrastructure
- **Eureka Dashboard:** [http://localhost:8761](http://localhost:8761)
- **RabbitMQ Management:** [http://localhost:15672](http://localhost:15672) (User/Pass: `rabbitmq`/`rabbitmq`)
- **Config Server Health:** `curl http://localhost:8888/actuator/health`

---

### 3. Run Option B: Full Stack in Docker

Build and run every component in containerized mode (including all Spring microservices) with a single command:

```bash
docker compose --env-file .env.dev -f docker-compose.dev.yml --profile full up -d --build
```

**View streaming logs:**
```bash
docker compose --env-file .env.dev -f docker-compose.dev.yml --profile full logs -f
```

**Stop full stack:**
```bash
docker compose --env-file .env.dev -f docker-compose.dev.yml --profile full down
```

---

### 4. Run Option C: Production Deployment

Simulate the production environment with restricted host bindings (`127.0.0.1`), memory resource constraints, and production configuration profiles:

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build
```

---

## ⚙️ Spring Profiles & Configuration Hierarchy

| Profile | Purpose | DB & Service Resolution | Typical Host |
|---|---|---|---|
| `dev` | Hybrid Development | `localhost` ports (`localhost:5432`, `localhost:27017`) | Local IDE / CLI |
| `docker` | Full Compose Dev | Docker internal DNS (`postgres`, `mongodb`, `rabbitmq`) | Docker Containers |
| `prod` | Production Simulation | Docker DNS + Strict DDL validation (`validate`) | Cloud / Server |

Configuration resolution flow:
1. Microservice starts with base `application.yml` and connects to `configserver:8888`.
2. Config Server merges `classpath:/config/shared/application-{profile}.yml` and `classpath:/config/{service}/application-{profile}.yml`.
3. Properties are injected into Spring Environment.

---

## 📬 Event-Driven Messaging Topology

The messaging layer runs on RabbitMQ using pre-declared topology defined in `docker/rabbitmq/definitions.json`:

```
┌─────────────────────────────────────────────────────────────┐
│                      RABBITMQ BROKER                        │
│                                                             │
│  [Exchange]                [Routing Key]          [Queue]   │
│  fitness.exchange ───────> activity.tracking ────> activity.queue
│  (Type: direct, durable)                           (durable)│
└─────────────────────────────────────────────────────────────┘
          ▲                                             │
          │ Published by                                │ Consumed by
┌─────────────────────────┐                   ┌─────────────────────────┐
│     Activity Service    │                   │       AI Service        │
└─────────────────────────┘                   └─────────────────────────┘
```

---

## ❓ Troubleshooting & FAQ

### 1. `WARN: The "POSTGRES_USER" variable is not set`
**Cause:** Ran `docker compose` without specifying the `--env-file` flag.  
**Fix:** Always pass `--env-file .env.dev`:
```bash
docker compose --env-file .env.dev -f docker-compose.dev.yml up -d
```

### 2. `Connection refused: localhost:5432` or `Connection refused: localhost:27017`
**Cause:** Docker infrastructure is not running.  
**Fix:** Run `docker compose --env-file .env.dev -f docker-compose.dev.yml up -d` before starting Java services locally.

### 3. Databases were not created on first boot
**Cause:** Init scripts in `docker/*/init` were not executable before the first container volume creation.  
**Fix:**
```bash
chmod +x docker/postgres/init/*.sh docker/mongodb/init/*.sh docker/rabbitmq/init/*.sh
docker compose --env-file .env.dev -f docker-compose.dev.yml down -v
docker compose --env-file .env.dev -f docker-compose.dev.yml up -d
```

### 4. Gemini API returns 403 / 400
**Cause:** Missing or invalid `GEMINI_API_KEY` in `.env.dev`.  
**Fix:** Obtain a free key from Google AI Studio and update `.env.dev`.

---

## 📂 Service Documentation Index

For in-depth details on each individual microservice, see:

- 🚪 [API Gateway Service Documentation](./gatewayservice/README.md)
- ⚙️ [Config Server Documentation](./configserver/README.md)
- 🔭 [Eureka Discovery Server Documentation](./eurekaservice/README.md)
- 👤 [User Service Documentation](./userservice/README.md)
- 🏃 [Activity Tracking Service Documentation](./activityservice/README.md)
- 🤖 [AI Recommendation Service Documentation](./aiservice/README.md)

---

## 📄 License
This project is licensed under the MIT License.
