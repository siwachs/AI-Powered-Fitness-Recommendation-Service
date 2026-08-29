# Activity Service (`activityservice`)

Fitness activity tracking microservice backed by MongoDB and Spring AMQP (RabbitMQ). Records user workout sessions, validates user existence against `userservice`, and publishes activity events to RabbitMQ for asynchronous AI analysis.

---

## Specifications

| Property | Details |
|---|---|
| **Direct Port** | `8082` |
| **Gateway Routed Path** | `http://localhost:8080/api/v1/activities/**` |
| **Database** | MongoDB (`activitydb` / collection: `activities`) |
| **Message Broker** | RabbitMQ (Producer to `fitness.exchange`) |
| **Inter-Service Communication** | LoadBalanced WebClient to `USER-SERVICE` |
| **Service Discovery** | Registers as `ACTIVITY-SERVICE` on Eureka |

---

## Domain Model and Supported Activity Types

### Supported `ActivityType` Enums
- `RUNNING`
- `WALKING`
- `CYCLING`
- `SWIMMING`
- `WEIGHT_TRAINING`
- `YOGA`
- `HIIT`
- `CARDIO`
- `STRETCHING`
- `OTHER`

### MongoDB Document Schema (`activities` collection)
```json
{
  "_id": "6d7293a7-e852-4fc8-9037-123456789abc",
  "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "type": "RUNNING",
  "duration": 45,
  "caloriesBurn": 420,
  "startTime": "2026-08-29T07:30:00",
  "additionalMetrics": {
    "distanceKm": 7.5,
    "avgHeartRate": 152,
    "maxHeartRate": 174,
    "paceMinPerKm": 6.0
  },
  "createdAt": "2026-08-29T08:20:00",
  "updatedAt": "2026-08-29T08:20:00"
}
```

---

## Event-Driven Architecture (RabbitMQ)

When an activity is successfully tracked and persisted:
1. Validates `userId` with `userservice` via `GET /api/v1/users/{keycloakId}/validate`.
2. Persists the workout document in MongoDB.
3. Automatically publishes the complete `Activity` object as JSON to:
   - **Exchange:** `fitness.exchange` (type: `direct`)
   - **Routing Key:** `activity.tracking`
   - **Bound Queue:** `activity.queue` (consumed asynchronously by `aiservice`)

---

## REST API Reference

### 1. Track a New Workout Activity
Validates the user, stores the activity in MongoDB, and triggers AI analysis via RabbitMQ.

- **URL:** `/api/v1/activities`
- **Method:** `POST`
- **Headers:**
  - `Content-Type: application/json`
  - `X-User-ID: <keycloak-user-uuid>` (Automatically populated by Gateway when using Bearer token)

#### Request Body
```json
{
  "type": "RUNNING",
  "duration": 45,
  "caloriesBurn": 420,
  "startTime": "2026-08-29T07:30:00",
  "additionalMetrics": {
    "distanceKm": 7.5,
    "avgHeartRate": 152,
    "maxHeartRate": 174,
    "paceMinPerKm": 6.0,
    "elevationGainMeters": 65
  }
}
```

#### Field Specifications
| Field | Type | Description |
|---|---|---|
| `type` | String (Enum) | Activity type (e.g. `RUNNING`, `WEIGHT_TRAINING`, `HIIT`) |
| `duration` | Integer | Workout duration in minutes |
| `caloriesBurn` | Integer | Estimated calories burned |
| `startTime` | ISO-8601 String | When the workout began (`YYYY-MM-DDTHH:mm:ss`) |
| `additionalMetrics` | Map / JSON Object | Flexible custom metrics (heart rate, distance, sets, cadence, etc.) |

#### Response (`200 OK`)
```json
{
  "id": "6d7293a7-e852-4fc8-9037-123456789abc",
  "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "type": "RUNNING",
  "duration": 45,
  "caloriesBurn": 420,
  "startTime": "2026-08-29T07:30:00",
  "additionalMetrics": {
    "distanceKm": 7.5,
    "avgHeartRate": 152,
    "maxHeartRate": 174,
    "paceMinPerKm": 6.0,
    "elevationGainMeters": 65
  },
  "createdAt": "2026-08-29T08:20:00",
  "updatedAt": "2026-08-29T08:20:00"
}
```

#### Direct cURL Example
```bash
curl -X POST http://localhost:8082/api/v1/activities \
  -H "Content-Type: application/json" \
  -H "X-User-ID: 3fa85f64-5717-4562-b3fc-2c963f66afa6" \
  -d '{
    "type": "RUNNING",
    "duration": 45,
    "caloriesBurn": 420,
    "startTime": "2026-08-29T07:30:00",
    "additionalMetrics": {
      "distanceKm": 7.5,
      "avgHeartRate": 152
    }
  }'
```

---

### 2. Get All Activities for Current User
Fetches all recorded workouts for the user specified in the header.

- **URL:** `/api/v1/activities`
- **Method:** `GET`
- **Headers:**
  - `X-User-ID: <keycloak-user-uuid>`

#### Response (`200 OK`)
```json
[
  {
    "id": "6d7293a7-e852-4fc8-9037-123456789abc",
    "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "type": "RUNNING",
    "duration": 45,
    "caloriesBurn": 420,
    "startTime": "2026-08-29T07:30:00",
    "additionalMetrics": {
      "distanceKm": 7.5,
      "avgHeartRate": 152
    },
    "createdAt": "2026-08-29T08:20:00",
    "updatedAt": "2026-08-29T08:20:00"
  }
]
```

#### cURL Example
```bash
curl -X GET http://localhost:8082/api/v1/activities \
  -H "X-User-ID: 3fa85f64-5717-4562-b3fc-2c963f66afa6"
```

---

### 3. Get Activity by ID
Retrieves details of a specific workout by its unique activity UUID.

- **URL:** `/api/v1/activities/{activityId}`
- **Method:** `GET`

#### Response (`200 OK`)
```json
{
  "id": "6d7293a7-e852-4fc8-9037-123456789abc",
  "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "type": "RUNNING",
  "duration": 45,
  "caloriesBurn": 420,
  "startTime": "2026-08-29T07:30:00",
  "additionalMetrics": {
    "distanceKm": 7.5
  },
  "createdAt": "2026-08-29T08:20:00",
  "updatedAt": "2026-08-29T08:20:00"
}
```

#### cURL Example
```bash
curl -X GET http://localhost:8082/api/v1/activities/6d7293a7-e852-4fc8-9037-123456789abc
```

---

## How to Run

### Hybrid Dev Mode
```bash
cd activityservice
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Docker Mode
```bash
docker compose --env-file .env.dev -f docker-compose.dev.yml --profile full up -d activity-service
```
