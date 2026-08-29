# User Service (`userservice`)

User management microservice backed by PostgreSQL and Spring Data JPA. Manages user identity profiles, Keycloak user synchronization, and user existence validation for other services.

---

## Specifications

| Property | Details |
|---|---|
| **Direct Port** | `8081` |
| **Gateway Routed Path** | `http://localhost:8080/api/v1/users/**` |
| **Database** | PostgreSQL (`userdb`) |
| **Service Discovery** | Registers as `USER-SERVICE` on Eureka |
| **Configuration** | Fetches from Config Server (`http://localhost:8888`) |

---

## Database Model and Schema

**Table:** `users`

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `UUID` | Primary Key, Auto-generated | Internal database identifier |
| `keycloak_id` | `UUID` | Nullable, Indexed | Subject identifier (`sub`) from Keycloak |
| `email` | `VARCHAR(255)` | Unique, Not Null, Indexed (`idx_email`) | User email address |
| `first_name` | `VARCHAR(255)` | Nullable | User's first name |
| `last_name` | `VARCHAR(255)` | Nullable | User's last name |
| `role` | `VARCHAR(20)` | Default: `USER` | Role (`USER`, `ADMIN`) |
| `created_at` | `TIMESTAMP` | Auto-generated (`@CreationTimestamp`) | Record creation timestamp |
| `updated_at` | `TIMESTAMP` | Auto-generated (`@UpdateTimestamp`) | Record last updated timestamp |

---

## REST API Reference

### 1. Register / Sync User
Creates a new user profile or returns the existing profile if the email is already registered.

- **URL:** `/api/v1/users/register`
- **Method:** `POST`
- **Content-Type:** `application/json`

#### Request Body
```json
{
  "keycloakId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "email": "alex.smith@example.com",
  "firstName": "Alex",
  "lastName": "Smith"
}
```

#### Field Validation Rules
| Field | Type | Validation |
|---|---|---|
| `email` | String | `@NotBlank`, `@Email` format required |
| `keycloakId` | UUID | Optional |
| `firstName` | String | Optional |
| `lastName` | String | Optional |

#### Response (`201 Created` / `200 OK`)
```json
{
  "id": "e7b0a708-59c4-4b51-9257-2384cf22485f",
  "keycloakId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "email": "alex.smith@example.com",
  "firstName": "Alex",
  "lastName": "Smith",
  "createdAt": "2026-08-29T10:15:30",
  "updatedAt": "2026-08-29T10:15:30"
}
```

#### cURL Example
```bash
curl -X POST http://localhost:8081/api/v1/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "keycloakId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "email": "alex.smith@example.com",
    "firstName": "Alex",
    "lastName": "Smith"
  }'
```

---

### 2. Get User by Internal ID
Retrieves a user profile using their internal PostgreSQL UUID.

- **URL:** `/api/v1/users/{userId}`
- **Method:** `GET`

#### Response (`200 OK`)
```json
{
  "id": "e7b0a708-59c4-4b51-9257-2384cf22485f",
  "keycloakId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "email": "alex.smith@example.com",
  "firstName": "Alex",
  "lastName": "Smith",
  "createdAt": "2026-08-29T10:15:30",
  "updatedAt": "2026-08-29T10:15:30"
}
```

#### Error Response (`404 Not Found`)
```json
{
  "status": 404,
  "error": "NOT_FOUND",
  "message": "User not found with id: e7b0a708-59c4-4b51-9257-2384cf22485f",
  "timestamp": "2026-08-29T10:16:00",
  "validationErrors": []
}
```

#### cURL Example
```bash
curl -X GET http://localhost:8081/api/v1/users/e7b0a708-59c4-4b51-9257-2384cf22485f
```

---

### 3. Validate User by Keycloak ID
Checks if a user exists by their Keycloak UUID. Used by `activityservice` and `gatewayservice`.

- **URL:** `/api/v1/users/{keycloakId}/validate`
- **Method:** `GET`

#### Response (`200 OK`)
```json
true
```

#### cURL Example
```bash
curl -X GET http://localhost:8081/api/v1/users/3fa85f64-5717-4562-b3fc-2c963f66afa6/validate
```

---

## Running the Service

### Hybrid Dev Mode
```bash
cd userservice
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Docker Compose
```bash
docker compose --env-file .env.dev -f docker-compose.dev.yml --profile full up -d user-service
```
