# AI-Powered Fitness Recommendation Service

A microservices-based fitness platform built with **Spring Boot 3**, **Spring Cloud Netflix Eureka**, **PostgreSQL**,
and **MongoDB**. Services are containerized with Docker and orchestrated via Docker Compose for local development and
production simulation.

---

### Service overview

| Service           | Port | Database               | Description                      |
|-------------------|------|------------------------|----------------------------------|
| `eurekaservice`   | 8761 | —                      | Service discovery registry       |
| `userservice`     | 8081 | PostgreSQL (`userdb`)  | User registration and validation |
| `activityservice` | 8082 | MongoDB (`activitydb`) | Fitness activity tracking        |
| `aiservice`       | 8083 | MongoDB (`aidb`)       | AI-powered recommendations       |

### Database-per-service

Each microservice owns its database.

| Service          | DB engine  | Database name |
|------------------|------------|---------------|
| user-service     | PostgreSQL | `userdb`      |
| activity-service | MongoDB    | `activitydb`  |
| ai-service       | MongoDB    | `aidb`        |

---

## Prerequisites

- **Java 21**
- **Maven 3.9+** (or use included `./mvnw` in each service)
- **Docker** & **Docker Compose v2**
- An IDE (IntelliJ IDEA recommended) for hybrid development

---

## Project structure

```
AI-Powered-Fitness-Recommendation-Service/
├── docker-compose.dev.yml       # Dev: infra only, or full stack with --profile full
├── docker-compose.prod.yml      # Production / prod simulation
├── .env.example                 # Template — copy to .env.dev / .env.prod
├── .env.dev                     # Local dev secrets (gitignored)
├── .env.prod                    # Prod secrets (gitignored)
├── docker/
│   ├── postgres/init/           # Runs once on first Postgres boot
│   └── mongodb/init/            # Runs once on first Mongo boot
├── eurekaservice/
├── userservice/
├── activityservice/
└── aiservice/
```

---

## First-time setup

### 1. Create environment files

```bash
cp .env.example .env.dev
cp .env.example .env.prod
```

Edit `.env.dev` for local development and `.env.prod` with strong passwords before any production deploy.

### 2. Make init scripts executable

Init scripts in `docker/postgres/init/` and `docker/mongodb/init/` must be executable. They run **only on the first boot
** when the data volume is empty.

```bash
chmod +x docker/postgres/init/*.sh
chmod +x docker/mongodb/init/*.sh
```

### 3. Verify Docker is running

```bash
docker info
```

---

## Spring profiles

Each runtime mode uses a different Spring profile. Config files follow the pattern `application-{profile}.yml`.

| Profile  | When to use                                     | Config source                                    |
|----------|-------------------------------------------------|--------------------------------------------------|
| `dev`    | Hybrid dev — apps run in IDE, infra in Docker   | `application-dev.yml` (hardcoded `localhost`)    |
| `docker` | Full stack in Docker Compose (`--profile full`) | `application-docker.yml` + env vars from compose |
| `prod`   | Production compose                              | `application-prod.yml` + env vars from compose   |

**Do not** set `spring.profiles.active` inside `application.yml`. Pass it via IDE run config, compose, or CLI:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

---

## Development workflows

### Option A — Hybrid dev (recommended for daily work)

Run **infrastructure in Docker**, run **Spring apps in your IDE** with profile `dev`. Fastest feedback loop for
debugging and hot reload.

**Step 1 — Start infrastructure**

```bash
docker compose -f docker-compose.dev.yml up -d
```

This starts:

- PostgreSQL (`localhost:5432`)
- MongoDB (`localhost:27017`)
- Eureka (`http://localhost:8761`)

**Step 2 — Run each service in IDE**

| Service         | Main class                   | Spring profile  | Port |
|-----------------|------------------------------|-----------------|------|
| eurekaservice   | `EurekaserviceApplication`   | *(none needed)* | 8761 |
| userservice     | `UserserviceApplication`     | `dev`           | 8081 |
| activityservice | `ActivityserviceApplication` | `dev`           | 8082 |
| aiservice       | `AiserviceApplication`       | `dev`           | 8083 |

IntelliJ: **Run → Edit Configurations → Active profiles: `dev`**

Or from CLI inside each service directory:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

**Step 3 — Verify**

```bash
# Eureka dashboard
open http://localhost:8761

# Health checks
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
```

**Stop infrastructure**

```bash
docker compose -f docker-compose.dev.yml down
```

---

### Option B — Full Docker dev (full stack in containers)

Everything runs in Docker. Uses Spring profile `docker` (set automatically by compose). Good for integration testing and
verifying container parity.

```bash
docker compose -f docker-compose.dev.yml --profile full up -d --build
```

This additionally starts:

- `user-service` → `localhost:8081`
- `activity-service` → `localhost:8082`
- `ai-service` → `localhost:8083`

**Watch logs**

```bash
docker compose -f docker-compose.dev.yml --profile full logs -f
```

**Rebuild a single service after code changes**

```bash
docker compose -f docker-compose.dev.yml --profile full up -d --build user-service
```

**Stop full stack**

```bash
docker compose -f docker-compose.dev.yml --profile full down
```

**Startup order:** `activity-service` waits for `user-service` to be healthy before starting, because it validates users
via Eureka/WebClient at runtime.

---

## Production

### Local prod simulation

Build and run the full stack with production Spring settings (`validate` DDL, resource limits, localhost-only port
binding).

**Step 1 — Configure secrets**

```bash
cp .env.example .env.prod
# Edit .env.prod — replace all change_me / secret_here values
```

**Step 2 — Start**

```bash
docker compose -f docker-compose.prod.yml up -d --build
```

**Step 3 — Verify** (services bound to localhost only)

```bash
curl http://127.0.0.1:8081/actuator/health/readiness
curl http://127.0.0.1:8082/actuator/health/readiness
curl http://127.0.0.1:8083/actuator/health/readiness
```

Eureka is **not** exposed publicly in prod compose — it is reachable only inside the Docker network.

**Stop**

```bash
docker compose -f docker-compose.prod.yml down
```

### Production with pre-built images (CI/CD)

Build and push images in CI, then deploy by tag:

```bash
export IMAGE_TAG=1.0.0
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d
```

Image names (configured in `.env.prod`):

```
${DOCKER_REGISTRY}/fitness-eureka-server:${IMAGE_TAG}
${DOCKER_REGISTRY}/fitness-user-service:${IMAGE_TAG}
${DOCKER_REGISTRY}/fitness-activity-service:${IMAGE_TAG}
${DOCKER_REGISTRY}/fitness-ai-service:${IMAGE_TAG}
```

---

## API endpoints

### user-service (`8081`)

| Method | Path                              | Description                                       |
|--------|-----------------------------------|---------------------------------------------------|
| `POST` | `/api/v1/users/register`          | Register a new user                               |
| `GET`  | `/api/v1/users/{userId}`          | Get user by ID                                    |
| `GET`  | `/api/v1/users/{userId}/validate` | Validate user exists (called by activity-service) |

### activity-service (`8082`)

| Method | Path                               | Description                                 |
|--------|------------------------------------|---------------------------------------------|
| `POST` | `/api/v1/activities`               | Track a new activity (validates user first) |
| `GET`  | `/api/v1/activities?userId={uuid}` | List activities for a user                  |
| `GET`  | `/api/v1/activities/{activityId}`  | Get activity by ID                          |

### ai-service (`8083`)

Scaffold service — no REST controllers yet. Registers with Eureka and connects to MongoDB.

### Health (all services)

```
GET /actuator/health
GET /actuator/health/readiness
GET /actuator/health/liveness
```

---

## Useful commands

### Container status

```bash
docker compose -f docker-compose.dev.yml ps
docker compose -f docker-compose.dev.yml --profile full ps
docker compose -f docker-compose.prod.yml ps
```

### Logs for a specific service

```bash
docker compose -f docker-compose.dev.yml logs -f postgres
docker compose -f docker-compose.dev.yml --profile full logs -f user-service
```

### Reset databases (dev only — deletes all data)

Init scripts re-run only when volumes are empty:

```bash
docker compose -f docker-compose.dev.yml down -v
docker compose -f docker-compose.dev.yml up -d
```

For full stack:

```bash
docker compose -f docker-compose.dev.yml --profile full down -v
docker compose -f docker-compose.dev.yml --profile full up -d --build
```

### Verify databases were initialized

```bash
# PostgreSQL — should list userdb
docker exec fitness-postgres-dev psql -U postgres -c "\l"

# MongoDB — should list activitydb and aidb
docker exec fitness-mongodb-dev mongosh -u mongo -p mongo --authenticationDatabase admin --eval "show dbs"
```

### Build a Docker image manually

```bash
docker build -t fitness-user-service ./userservice
docker build -t fitness-activity-service ./activityservice
docker build -t fitness-ai-service ./aiservice
docker build -t fitness-eureka-server ./eurekaservice
```

---

## Environment variables

See [`.env.example`](.env.example) for the full list. Key groups:

| Group                                     | Used by            | Purpose                   |
|-------------------------------------------|--------------------|---------------------------|
| `POSTGRES_USER` / `POSTGRES_PASSWORD`     | Postgres container | Database admin            |
| `MONGO_ROOT_USER` / `MONGO_ROOT_PASSWORD` | Mongo container    | Mongo admin               |
| `USER_SERVICE_DB_*`                       | user-service       | PostgreSQL connection     |
| `ACTIVITY_SERVICE_MONGO_URI`              | activity-service   | MongoDB connection string |
| `AI_SERVICE_MONGO_URI`                    | ai-service         | MongoDB connection string |
| `EUREKA_URL`                              | All app services   | Eureka registry URL       |
| `DOCKER_REGISTRY` / `IMAGE_TAG`           | Prod compose       | Pre-built image tags      |

Init scripts read `USER_SERVICE_DB_*` and `ACTIVITY_SERVICE_DB_*` / `AI_SERVICE_DB_*` from the env file loaded into the
database containers via `env_file`.

---

## Troubleshooting

### Init scripts did not create databases

Init runs **once** on an empty volume. If you started Docker before making scripts executable:

```bash
chmod +x docker/postgres/init/*.sh docker/mongodb/init/*.sh
docker compose -f docker-compose.dev.yml down -v
docker compose -f docker-compose.dev.yml up -d
```

### Mongo / Postgres healthcheck failing

Ensure `.env.dev` or `.env.prod` has `MONGO_ROOT_USER` and `MONGO_ROOT_PASSWORD` set. Healthchecks authenticate with
those credentials.

### Activity service fails to validate users

- Confirm user-service is registered in Eureka: `http://localhost:8761`
- Wait ~30s after full stack startup for Eureka registration
- In full Docker mode, activity-service waits for user-service health — if calling too early from hybrid mode, start
  user-service first

### Port already in use

```bash
lsof -i :5432
lsof -i :8081
```

Stop conflicting processes or change published ports in compose.

### Spring profile wrong

| Symptom                                              | Fix                                       |
|------------------------------------------------------|-------------------------------------------|
| App tries to connect to `postgres` hostname from IDE | Use profile `dev`, not `docker`           |
| App tries `localhost` inside container               | Use profile `docker` or `prod`, not `dev` |

---

## Tech stack

- Java 21, Spring Boot 3.5, Spring Cloud 2025
- Spring Cloud Netflix Eureka
- PostgreSQL 16, MongoDB 8
- Docker multi-stage builds (Maven → JRE)
- Spring Boot Actuator (health/readiness/liveness)

---

## Workflow quick reference

| Mode                   | Command                                                                 | Spring profile       |
|------------------------|-------------------------------------------------------------------------|----------------------|
| **Hybrid dev**         | `docker compose -f docker-compose.dev.yml up -d`                        | `dev` in IDE         |
| **Full Docker dev**    | `docker compose -f docker-compose.dev.yml --profile full up -d --build` | `docker` (automatic) |
| **Prod local**         | `docker compose -f docker-compose.prod.yml up -d --build`               | `prod` (automatic)   |
| **Prod with registry** | `IMAGE_TAG=x docker compose -f docker-compose.prod.yml up -d`           | `prod` (automatic)   |
