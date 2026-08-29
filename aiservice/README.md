# AI Recommendation Service (`aiservice`)

AI-driven fitness intelligence microservice powered by **Google Gemini AI**, **Spring AMQP (RabbitMQ)**, and **MongoDB**. Asynchronously generates personalized coaching recommendations, performance analyses, progression suggestions, and safety tips for completed workouts.

---

## Architectural Flow

```
   ┌──────────────────────────────────────────────┐
   │            Activity Service                  │
   │  Publishes workout event to RabbitMQ         │
   └──────────────────────┬───────────────────────┘
                          │
                          ▼
            [RabbitMQ: activity.queue]
                          │
                          ▼
   ┌──────────────────────────────────────────────┐
   │                  AI SERVICE                  │
   │                                              │
   │  1. @RabbitListener consumes Activity event │
   │  2. Builds contextual prompt                 │
   │  3. Invokes Google Gemini API (WebClient)    │
   │  4. Extracts JSON analysis, improvements,    │
   │     suggestions, & safety guidelines         │
   │  5. Persists Recommendation in MongoDB       │
   └──────────────────────┬───────────────────────┘
                          │
                          ▼
             [MongoDB: recommendations]
                          │
                          ▼
              Queried via REST APIs:
   - GET /api/v1/recommendations/user/{userId}
   - GET /api/v1/recommendations/activity/{activityId}
```

---

## Specifications

| Property | Details |
|---|---|
| **Direct Port** | `8083` |
| **Gateway Routed Path** | `http://localhost:8080/api/v1/recommendations/**` |
| **Database** | MongoDB (`recommendationdb` / collection: `recommendations`) |
| **Message Broker** | RabbitMQ (Consumer on `activity.queue`) |
| **AI LLM Engine** | Google Gemini Generative Language API |
| **Service Discovery** | Registers as `AI-SERVICE` on Eureka |

---

## Google Gemini AI Prompt & Analysis Pipeline

### 1. AI Prompt Construction
When an activity event arrives, `ActivityAiService` constructs a prompt requesting structured JSON:
```text
Analyze this fitness activity and provide detailed recommendations in the following format:
{
  "analysis": {
    "overall": "Overall analysis here",
    "pace": "Pace analysis here",
    "heartRate": "Heart rate analysis here",
    "caloriesBurned": "Calories Burned here"
  },
  "improvements": [
    { "area": "Cardio Stamina", "recommendation": "Maintain tempo for +5 mins next session" }
  ],
  "suggestions": [
    { "workout": "Interval Speed Run", "description": "8x400m sprints with 90s recovery" }
  ],
  "safety": [
    "Hydrate before running in high temperatures",
    "Dynamic stretches for hamstrings"
  ]
}
```

### 2. Response Parsing & Normalization
The service strips markdown formatting (e.g. ```` ```json ```` fences), safely parses JSON nodes, formats composite analysis strings, and stores structured lists for actionable insights.

---

## MongoDB Document Schema (`recommendations` collection)

```json
{
  "_id": "b18a204f-3c87-4184-9092-23fba930d433",
  "activityId": "6d7293a7-e852-4fc8-9037-123456789abc",
  "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "activityType": "RUNNING",
  "recommendation": "Overall: Great endurance pace maintained throughout the 45-minute session.\n\nPace: Consistent 6.0 min/km pace.\n\nHeart Rate: Average heart rate of 152 bpm shows high aerobic zone efficiency.\n\nCalories Burned: 420 kcal matches target intensity.\n\n",
  "improvements": [
    "Pacing: Try negative splits on your final kilometer to build closing speed.",
    "Cadence: Aim for 170-180 strides per minute to reduce impact stress."
  ],
  "suggestions": [
    "Tempo Run: 30 minutes at 5:30 min/km pace with warm-up and cool-down.",
    "Cross-Training: 40-minute cycling session for active recovery."
  ],
  "safety": [
    "Ensure adequate hydration after workouts exceeding 40 minutes.",
    "Perform post-run calf and hamstring static stretches."
  ],
  "createdAt": "2026-08-29T08:20:05"
}
```

---

## REST API Reference

### 1. Get Recommendations for a User
Retrieves all generated AI recommendations for a specific user.

- **URL:** `/api/v1/recommendations/user/{userId}`
- **Method:** `GET`

#### Response (`200 OK`)
```json
[
  {
    "id": "b18a204f-3c87-4184-9092-23fba930d433",
    "activityId": "6d7293a7-e852-4fc8-9037-123456789abc",
    "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "activityType": "RUNNING",
    "recommendation": "Overall: Strong aerobic pace throughout the session.\n\n...",
    "improvements": [
      "Pacing: Try negative splits on your final kilometer."
    ],
    "suggestions": [
      "Tempo Run: 30 minutes at high aerobic threshold."
    ],
    "safety": [
      "Ensure proper hydration and post-run stretching."
    ],
    "createdAt": "2026-08-29T08:20:05"
  }
]
```

#### cURL Example
```bash
curl -X GET http://localhost:8083/api/v1/recommendations/user/3fa85f64-5717-4562-b3fc-2c963f66afa6
```

---

### 2. Get Recommendation for a Specific Activity
Retrieves the AI recommendation generated for a specific workout ID.

- **URL:** `/api/v1/recommendations/activity/{activityId}`
- **Method:** `GET`

#### Response (`200 OK`)
```json
{
  "id": "b18a204f-3c87-4184-9092-23fba930d433",
  "activityId": "6d7293a7-e852-4fc8-9037-123456789abc",
  "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "activityType": "RUNNING",
  "recommendation": "Overall: Great endurance pace maintained throughout the 45-minute session.\n\nPace: Consistent 6.0 min/km pace.\n\nHeart Rate: Average heart rate of 152 bpm shows high aerobic zone efficiency.\n\nCalories Burned: 420 kcal matches target intensity.\n\n",
  "improvements": [
    "Pacing: Try negative splits on your final kilometer to build closing speed.",
    "Cadence: Aim for 170-180 strides per minute to reduce impact stress."
  ],
  "suggestions": [
    "Tempo Run: 30 minutes at 5:30 min/km pace with warm-up and cool-down.",
    "Cross-Training: 40-minute cycling session for active recovery."
  ],
  "safety": [
    "Ensure adequate hydration after workouts exceeding 40 minutes.",
    "Perform post-run calf and hamstring static stretches."
  ],
  "createdAt": "2026-08-29T08:20:05"
}
```

#### cURL Example
```bash
curl -X GET http://localhost:8083/api/v1/recommendations/activity/6d7293a7-e852-4fc8-9037-123456789abc
```

---

## Environment Variables

| Variable | Description | Example |
|---|---|---|
| `AI_SERVICE_MONGO_URI` | MongoDB Connection URI | `mongodb://mongo:mongo@localhost:27017/recommendationdb?authSource=admin` |
| `GEMINI_API_URL` | Google Gemini API Base URL | `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent` |
| `GEMINI_API_KEY` | Google Gemini API Key | `AIzaSy...` |
| `RABBITMQ_HOST` | RabbitMQ Hostname | `localhost` or `rabbitmq` |
| `RABBITMQ_PORT` | RabbitMQ Port | `5672` |
| `CONFIG_SERVER_URL` | Config Server URL | `http://config-server:8888` |
| `EUREKA_URL` | Eureka Registry URL | `http://eureka-server:8761/eureka/` |

---

## How to Run

### Hybrid Dev Mode
```bash
cd aiservice
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Docker Mode
```bash
docker compose --env-file .env.dev -f docker-compose.dev.yml --profile full up -d ai-service
```
