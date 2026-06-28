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
| `activityservice` | 8082 | MongoDB (`activity`) | Fitness activity tracking        |
| `aiservice`       | 8083 | MongoDB (`recommendation`) | AI-powered recommendations       |

### Database-per-service

Each microservice owns its database.

| Service          | DB engine  | Database name |
|------------------|------------|---------------|
| user-service     | PostgreSQL | `userdb`      |
| activity-service | MongoDB    | `activity`        |
| ai-service       | MongoDB    | `recommendation`  |

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
├── docker-compose.dev.yml
├── docker-compose.prod.yml
├── .env.example / .env.dev / .env.prod
├── docker/postgres/init/          # DB init scripts
├── docker/mongodb/init/
├── userservice/pom.xml            # ← independent Maven project (no root pom)
├── activityservice/pom.xml
├── eurekaservice/pom.xml
└── aiservice/pom.xml
```

> **Note:** There is no parent `pom.xml`. In IntelliJ, import each service via the **Maven** tool window (four separate Maven roots).

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

## Docker Compose and environment files

This project uses **`.env.dev`** and **`.env.prod`** — not a root **`.env`** file. Docker Compose treats these differently:

| Mechanism | Purpose | File used |
|-----------|---------|-----------|
| **`--env-file`** flag | Substitutes `${VAR}` in the compose YAML (healthchecks, `environment:` blocks) | `.env.dev` or `.env.prod` — **you must pass this** |
| **`env_file:`** on each service | Injects variables into the **container** at runtime | Same file, declared per service |

Compose **does not** auto-load `.env.dev`. It only auto-loads a file named exactly `.env`. Without `--env-file`, you will see warnings like:

```
WARN: The "POSTGRES_USER" variable is not set. Defaulting to a blank string.
```

That breaks healthchecks and can pass empty credentials into containers.

**Always include `--env-file`:**

```bash
# Development
docker compose --env-file .env.dev -f docker-compose.dev.yml <command>

# Production
docker compose --env-file .env.prod -f docker-compose.prod.yml <command>
```

---

## Spring profiles

Each runtime mode uses a different Spring profile. Spring Boot loads **`application.yml`** (base) plus **`application-{profile}.yml`** when a profile is active.

### Profile summary

| Profile  | When to use                                     | How profile is set                              | Config file loaded        |
|----------|-------------------------------------------------|-------------------------------------------------|---------------------------|
| `dev`    | Hybrid dev — apps in IDE, infra in Docker       | IDE run config or Maven `-Dspring-boot.run.profiles=dev` | `application-dev.yml`     |
| `docker` | Full stack in Docker Compose (`--profile full`) | Compose sets `SPRING_PROFILES_ACTIVE=docker`  | `application-docker.yml`  |
| `prod`   | Production compose                              | Compose sets `SPRING_PROFILES_ACTIVE=prod`      | `application-prod.yml`    |

### Config files per service

| Service | Base file | `dev` | `docker` | `prod` |
|---------|-----------|-------|----------|--------|
| eurekaservice | `application.yaml` | — (same base) | — | — |
| userservice | `application.yml` | `application-dev.yml` | `application-docker.yml` | `application-prod.yml` |
| activityservice | `application.yaml` | `application-dev.yml` | `application-docker.yml` | `application-prod.yml` |
| aiservice | `application.yaml` | `application-dev.yml` | `application-docker.yml` | `application-prod.yml` |

### What each profile connects to

| Profile | DB / Eureka hostnames | Credentials |
|---------|----------------------|-------------|
| `dev` | `localhost` (hardcoded in YAML) | Hardcoded in YAML (`postgres`/`postgres`, `mongo`/`mongo`) |
| `docker` | Docker DNS (`postgres`, `mongodb`, `eureka-server`) | From `.env.dev` via compose |
| `prod` | Docker DNS (same as docker) | From `.env.prod` via compose |

**Do not** set `spring.profiles.active` inside `application.yml`. Pass it explicitly:

```bash
# Maven (inside a service directory)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Packaged JAR
java -jar target/userservice-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev

# Environment variable (any shell / IDE)
export SPRING_PROFILES_ACTIVE=dev
./mvnw spring-boot:run
```

> **Important:** Use profile **`dev`** when running Java apps locally in your IDE. Profiles **`docker`** and **`prod`** require environment variables (`USER_SERVICE_DB_*`, `ACTIVITY_SERVICE_MONGO_URI`, etc.) that are normally supplied by Docker Compose — do not use them from the IDE unless you export those vars manually.

---

## Running Java services locally (detailed)

This section covers hybrid development: **Docker runs infrastructure**, **you run Spring Boot apps on the host** with profile **`dev`**.

### Architecture (hybrid dev)

```
┌──────────────────────────────── Docker ────────────────────────────────┐
│  postgres :5432    mongodb :27017    eureka-server :8761              │
└──────────────────────────────────────────────────────────────────────┘
         ▲                  ▲                    ▲
         │ localhost        │ localhost          │ localhost
         │                  │                    │
┌────────┴──────────────────┴────────────────────┴─────────────────────┐
│  Your Mac — Spring Boot (profile: dev)                               │
│  userservice :8081   activityservice :8082   aiservice :8083       │
└──────────────────────────────────────────────────────────────────────┘
```

Eureka runs **inside Docker** (started by compose). You do **not** need to run `eurekaservice` from the IDE in hybrid mode — doing so would conflict on port `8761`.

### Startup order

Start components in this order:

| Step | What | How |
|------|------|-----|
| 1 | PostgreSQL + MongoDB + Eureka | `docker compose --env-file .env.dev -f docker-compose.dev.yml up -d` |
| 2 | Wait for infra | ~10–30s — check `docker compose ... ps` shows healthy |
| 3 | `userservice` | IDE or Maven, profile `dev`, port 8081 |
| 4 | `activityservice` | IDE or Maven, profile `dev`, port 8082 |
| 5 | `aiservice` | IDE or Maven, profile `dev`, port 8083 |

Start **user-service before testing activity-service** — activity calls user-service via Eureka to validate users.

### Service reference (copy-paste ready)

| Service | Module directory | Main class | Profile | Port |
|---------|------------------|------------|---------|------|
| eurekaservice | `eurekaservice/` | `com.fitness.eurekaservice.EurekaserviceApplication` | *(Docker only in hybrid)* | 8761 |
| userservice | `userservice/` | `com.fitness.userservice.UserserviceApplication` | `dev` | 8081 |
| activityservice | `activityservice/` | `com.fitness.activityservice.ActivityserviceApplication` | `dev` | 8082 |
| aiservice | `aiservice/` | `com.fitness.aiservice.AiserviceApplication` | `dev` | 8083 |

---

### Option 1 — IntelliJ IDEA

This repo has **four separate Maven projects** (`userservice/`, `activityservice/`, `eurekaservice/`, `aiservice/`). There is **no root `pom.xml`**. IntelliJ links each `pom.xml` individually — you will see four entries in the **Maven** tool window, not one parent module.

---

#### Step 1 — Open the project (one time)

1. **File → Open…**
2. Select the **repo root folder** `AI-Powered-Fitness-Recommendation-Service` (the folder that contains `docker-compose.dev.yml`)
3. Click **Open** → choose **Trust Project** if prompted
4. Wait for indexing to finish (progress bar bottom-right)

---

#### Step 2 — Set Java 21 (one time)

1. **File → Project Structure…** (`⌘ ;` on Mac)
2. **Project** (left sidebar):
   - **SDK:** `21` (e.g. `homebrew-21`, `temurin-21`, or `openjdk-21`)
   - **Language level:** `21`
3. If no Java 21 listed → **Edit** → **Add SDK → Download JDK…** → version **21**
4. Click **Apply** → **OK**

---

#### Step 3 — Load all Maven services (one time)

1. Open **Maven** tool window: **View → Tool Windows → Maven**
2. You should see four roots:
   - `userservice`
   - `activityservice`
   - `eurekaservice`
   - `aiservice`

**If Maven window is empty or missing projects:**

1. In Maven tool window click **+** (Add Maven Projects)
2. Select all four files (hold `⌘` / `Ctrl`):
   - `userservice/pom.xml`
   - `activityservice/pom.xml`
   - `eurekaservice/pom.xml`
   - `aiservice/pom.xml`
3. Click **Open**
4. Click the **Reload All Maven Projects** icon (circular arrows)

Wait until download/index completes (no progress spinner in Maven window).

---

#### Step 4 — Run a service (pick ONE method below)

Start Docker infra **before** running any app service:

```bash
docker compose --env-file .env.dev -f docker-compose.dev.yml up -d
```

---

##### Method A — Fastest: run from the main class (recommended)

1. In the **Project** tool window, navigate to e.g.  
   `userservice → src → main → java → com.fitness.userservice → UserserviceApplication.java`
2. Open the file
3. Click the **green ▶** gutter icon next to `public class UserserviceApplication`  
   — or right-click inside the file → **Run 'UserserviceApplication'**
4. First run will fail or use wrong config — immediately: **Run → Edit Configurations…**
5. Select the config that was just created and set the profile (see **Method B** or **Method C** below depending on your edition)
6. Repeat for:
   - `activityservice/.../ActivityserviceApplication.java`
   - `aiservice/.../AiserviceApplication.java`

> Do **not** run `EurekaserviceApplication` in hybrid mode — Eureka already runs in Docker on port **8761**.

---

##### Method B — Spring Boot run configuration (IntelliJ IDEA **Ultimate**)

1. **Run → Edit Configurations…**
2. Click **+** → **Spring Boot**  
   *(If you don't see "Spring Boot", use Method C — you likely have Community Edition)*
3. Configure **userservice**:

| Field | Value |
|-------|-------|
| **Name** | `userservice (dev)` |
| **Build and run → JRE** | Project SDK (Java 21) |
| **Build and run → module** | **`userservice`** ← must NOT be `AI-Powered-Fitness-Recommendation-Service` |
| **Main class** | `com.fitness.userservice.UserserviceApplication` |
| **Active profiles** | `dev` |
| **Working directory** | `$MODULE_WORKING_DIR$` |

4. **Apply** → **OK**
5. Duplicate for **activityservice** and **aiservice**:

| Name | Module | Main class | Active profiles |
|------|--------|------------|-----------------|
| `activityservice (dev)` | `activityservice` | `com.fitness.activityservice.ActivityserviceApplication` | `dev` |
| `aiservice (dev)` | `aiservice` | `com.fitness.aiservice.AiserviceApplication` | `dev` |

---

##### Method C — Application run configuration (IntelliJ IDEA **Community**)

Community Edition has **no "Spring Boot"** template and **no "Active profiles"** field. Use **Application** + VM options instead:

1. **Run → Edit Configurations…**
2. Click **+** → **Application**
3. Configure **userservice**:

| Field | Value |
|-------|-------|
| **Name** | `userservice (dev)` |
| **Build and run → module** | **`userservice`** |
| **Main class** | `com.fitness.userservice.UserserviceApplication` |
| **VM options** | `-Dspring.profiles.active=dev` |
| **Working directory** | `$MODULE_DIR$/userservice` or `$MODULE_WORKING_DIR$` |

4. **Apply** → **OK**
5. Repeat for `activityservice` and `aiservice` with their main classes and the same VM option.

Alternative to VM options — **Environment variables** field:

```
SPRING_PROFILES_ACTIVE=dev
```

---

##### Method D — Run via Maven tool window

No run configuration needed:

1. Maven tool window → expand **userservice → Plugins → spring-boot**
2. Right-click **spring-boot:run → Modify Run Configuration…**
3. **Command line** field, add:

```
-Dspring-boot.run.profiles=dev
```

4. Run **spring-boot:run**
5. Repeat for `activityservice` and `aiservice`

---

#### Step 5 — Start services in order

Use the run configuration dropdown (top-right toolbar):

1. Run **`userservice (dev)`** — wait until console shows `Started UserserviceApplication`
2. Run **`activityservice (dev)`**
3. Run **`aiservice (dev)`**

#### Step 6 — Verify

1. Eureka dashboard: [http://localhost:8761](http://localhost:8761)  
   Registered: `USER-SERVICE`, `ACTIVITY-SERVICE`, `AI-SERVICE`
2. Health checks:

```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
```

---

#### IntelliJ troubleshooting

| Problem | Cause | Fix |
|---------|-------|-----|
| No **Module** named `userservice` in dropdown | Maven project not imported | Maven window → **+** → add `userservice/pom.xml` → Reload |
| Only module is `AI-Powered-Fitness-Recommendation-Service` | Wrong module selected | Pick **`userservice`**, not the root project shell |
| No **Active profiles** field | Community Edition | Use Method C: VM options `-Dspring.profiles.active=dev` |
| No **Spring Boot** in **+** menu | Community Edition | Use **Application** (Method C) or Maven (Method D) |
| `Connection refused: localhost:5432` | Docker infra not running | Run `docker compose --env-file .env.dev -f docker-compose.dev.yml up -d` |
| `Connection refused: postgres:5432` | Wrong profile (`docker`/`prod`) | Set profile to **`dev`** |
| Port 8761 already in use | Running Eureka in IDE + Docker | Stop IDE Eureka — hybrid uses Docker Eureka only |
| Lombok errors / cannot find symbol | Annotation processing off | **Settings → Build → Compiler → Annotation Processors → Enable annotation processing** |
| Main class not found | Opened subfolder not root | **File → Open** the repo root, not `userservice/` alone |

---

### Option 2 — Maven CLI

Open **four separate terminal tabs** (infra first, then one per app service).

**Terminal 1 — Infrastructure**

```bash
cd AI-Powered-Fitness-Recommendation-Service
docker compose --env-file .env.dev -f docker-compose.dev.yml up -d
```

**Terminal 2 — user-service**

```bash
cd userservice
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

**Terminal 3 — activity-service**

```bash
cd activityservice
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

**Terminal 4 — ai-service**

```bash
cd aiservice
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Stop a service with `Ctrl+C` in its terminal.

---

### Option 3 — Packaged JAR

Build once, run with explicit profile:

```bash
cd userservice
./mvnw clean package -DskipTests
java -jar target/userservice-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

Repeat for each service (artifact names match `{artifactId}-0.0.1-SNAPSHOT.jar` from each `pom.xml`).

---

### Option 4 — VS Code

1. Install **Extension Pack for Java**
2. Open a service folder (e.g. `userservice/`)
3. Add to `.vscode/launch.json`:

```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "userservice (dev)",
      "request": "launch",
      "mainClass": "com.fitness.userservice.UserserviceApplication",
      "vmArgs": "-Dspring.profiles.active=dev"
    }
  ]
}
```

4. Start Docker infra, then **Run → Start Debugging**

---

### Profiles `docker` and `prod` from the host (advanced)

These profiles read `${ENV_VAR}` placeholders. They are **designed for Docker Compose**, which sets both the profile and the variables:

```yaml
environment:
  SPRING_PROFILES_ACTIVE: docker   # or prod
  USER_SERVICE_DB_HOST: postgres
  ...
```

To run from the IDE with `docker` or `prod` you would need to export every variable first — not recommended. Use:

| Goal | Approach |
|------|----------|
| Daily coding | Profile **`dev`** + IDE |
| Test containers | `docker compose ... --profile full` (profile **`docker`**) |
| Prod simulation | `docker compose ... -f docker-compose.prod.yml` (profile **`prod`**) |

---

## Development workflows

### Option A — Hybrid dev (recommended for daily work)

Run **infrastructure in Docker**, run **Spring apps on your host** with profile `dev`. See [Running Java services locally (detailed)](#running-java-services-locally-detailed) for IntelliJ, Maven, and VS Code setup.

**Step 1 — Start infrastructure**

```bash
docker compose --env-file .env.dev -f docker-compose.dev.yml up -d
```

This starts in Docker:

- PostgreSQL → `localhost:5432`
- MongoDB → `localhost:27017`
- Eureka → `http://localhost:8761`

**Step 2 — Run app services in IntelliJ (profile `dev`)**

See **[Option 1 — IntelliJ IDEA](#option-1--intellij-idea)** for full setup (Community vs Ultimate, Maven import, run configs).

Quick summary:

| Service | Main class | IntelliJ module | Profile |
|---------|------------|-----------------|---------|
| userservice | `com.fitness.userservice.UserserviceApplication` | `userservice` | `dev` |
| activityservice | `com.fitness.activityservice.ActivityserviceApplication` | `activityservice` | `dev` |
| aiservice | `com.fitness.aiservice.AiserviceApplication` | `aiservice` | `dev` |

- **Ultimate:** Spring Boot run config → **Active profiles:** `dev`
- **Community:** Application run config → **VM options:** `-Dspring.profiles.active=dev`
- **Maven (either edition):** `userservice → Plugins → spring-boot → spring-boot:run` with `-Dspring-boot.run.profiles=dev`

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
docker compose --env-file .env.dev -f docker-compose.dev.yml down
```

---

### Option B — Full Docker dev (full stack in containers)

Everything runs in Docker — **no IDE or Maven needed** for app services. Compose sets Spring profile **`docker`** automatically via `SPRING_PROFILES_ACTIVE=docker`. Good for integration testing and verifying container parity.

```bash
docker compose --env-file .env.dev -f docker-compose.dev.yml --profile full up -d --build
```

This additionally starts:

- `user-service` → `localhost:8081`
- `activity-service` → `localhost:8082`
- `ai-service` → `localhost:8083`

**Watch logs**

```bash
docker compose --env-file .env.dev -f docker-compose.dev.yml --profile full logs -f
```

**Rebuild a single service after code changes**

```bash
docker compose --env-file .env.dev -f docker-compose.dev.yml --profile full up -d --build user-service
```

**Stop full stack**

```bash
docker compose --env-file .env.dev -f docker-compose.dev.yml --profile full down
```

**Startup order:** `activity-service` waits for `user-service` to be healthy before starting, because it validates users
via Eureka/WebClient at runtime.

---

## Production

Spring profile **`prod`** is set automatically by prod compose (`SPRING_PROFILES_ACTIVE=prod`). Do not run prod profile from the IDE unless you export all required env vars manually — use compose instead.

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
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build
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
docker compose --env-file .env.prod -f docker-compose.prod.yml down
```

### Production with pre-built images (CI/CD)

Build and push images in CI, then deploy by tag:

```bash
export IMAGE_TAG=1.0.0
docker compose --env-file .env.prod -f docker-compose.prod.yml pull
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d
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
docker compose --env-file .env.dev -f docker-compose.dev.yml ps
docker compose --env-file .env.dev -f docker-compose.dev.yml --profile full ps
docker compose --env-file .env.prod -f docker-compose.prod.yml ps
```

### Logs for a specific service

```bash
docker compose --env-file .env.dev -f docker-compose.dev.yml logs -f postgres
docker compose --env-file .env.dev -f docker-compose.dev.yml --profile full logs -f user-service
```

### Reset databases (dev only — deletes all data)

Init scripts re-run only when volumes are empty:

```bash
docker compose --env-file .env.dev -f docker-compose.dev.yml down -v
docker compose --env-file .env.dev -f docker-compose.dev.yml up -d
```

For full stack:

```bash
docker compose --env-file .env.dev -f docker-compose.dev.yml --profile full down -v
docker compose --env-file .env.dev -f docker-compose.dev.yml --profile full up -d --build
```

### Verify databases were initialized

```bash
# PostgreSQL — should list userdb
docker exec fitness-postgres-dev psql -U postgres -c "\l"

# MongoDB — should list activity and recommendation
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

See [`.env.example`](.env.example) for the full list. Pass the matching file to compose with `--env-file` (see [Docker Compose and environment files](#docker-compose-and-environment-files)).

Key groups:

| Group                                     | Used by            | Purpose                   |
|-------------------------------------------|--------------------|---------------------------|
| `POSTGRES_USER` / `POSTGRES_PASSWORD`     | Postgres container | Database admin            |
| `MONGO_ROOT_USER` / `MONGO_ROOT_PASSWORD` | Mongo container    | Mongo admin               |
| `USER_SERVICE_DB_NAME`                    | Postgres init + user-service | PostgreSQL database name |
| `ACTIVITY_SERVICE_DB_NAME`                | Mongo init + activity-service | MongoDB database name |
| `AI_SERVICE_DB_NAME`                      | Mongo init + ai-service | MongoDB database name |
| `USER_SERVICE_DB_*`                       | user-service       | PostgreSQL connection     |
| `ACTIVITY_SERVICE_DB_USER/PASSWORD`       | Mongo init (prod)  | Created when URI uses that user |
| `AI_SERVICE_DB_USER/PASSWORD`             | Mongo init (prod)  | Created when URI uses that user |
| `ACTIVITY_SERVICE_MONGO_URI`              | activity-service   | MongoDB connection string |
| `AI_SERVICE_MONGO_URI`                    | ai-service         | MongoDB connection string |
| `EUREKA_URL`                              | All app services   | Eureka registry URL       |
| `DOCKER_REGISTRY` / `IMAGE_TAG`           | Prod compose       | Pre-built image tags      |

Init scripts read the same env vars as the apps (`*_DB_NAME`, `*_DB_USER`, `*_MONGO_URI`) via `env_file` on postgres/mongodb containers. Dev skips scoped user creation automatically (postgres admin user, mongo root in URI).

---

## Troubleshooting

### `variable is not set. Defaulting to a blank string`

You ran compose without `--env-file`. Compose only auto-loads `.env`, not `.env.dev`:

```bash
# Wrong
docker compose -f docker-compose.dev.yml up -d

# Correct
docker compose --env-file .env.dev -f docker-compose.dev.yml up -d
```

### Init scripts did not create databases

Init runs **once** on an empty volume. If you started Docker before making scripts executable:

```bash
chmod +x docker/postgres/init/*.sh docker/mongodb/init/*.sh
docker compose --env-file .env.dev -f docker-compose.dev.yml down -v
docker compose --env-file .env.dev -f docker-compose.dev.yml up -d
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

| Symptom | Likely cause | Fix |
|---------|--------------|-----|
| `Connection refused` to `postgres` or `mongodb` from IDE | Using `docker`/`prod` profile locally | Use profile **`dev`** |
| `Connection refused` to `localhost:5432` inside container | Using `dev` profile in Docker | Use **`docker`** or **`prod`** (set by compose) |
| `Could not resolve placeholder 'USER_SERVICE_DB_HOST'` | `docker`/`prod` profile without env vars | Run via compose, or export all vars |
| Port `8761` already in use | Eureka running in Docker + IDE | Stop one — hybrid dev uses Docker Eureka only |
| Services not in Eureka dashboard | Started apps before Eureka was up | Wait for infra healthy, restart app services |
| Activity validation fails | user-service not registered yet | Start user-service first; wait ~30s |

---

## Tech stack

- Java 21, Spring Boot 3.5, Spring Cloud 2025
- Spring Cloud Netflix Eureka
- PostgreSQL 16, MongoDB 8
- Docker multi-stage builds (Maven → JRE)
- Spring Boot Actuator (health/readiness/liveness)

---

## Workflow quick reference

| Mode | Docker command | Java / Spring profile | Where apps run |
|------|----------------|----------------------|----------------|
| **Hybrid dev** | `docker compose --env-file .env.dev -f docker-compose.dev.yml up -d` | **`dev`** in IDE or Maven | Host (IDE) |
| **Full Docker dev** | `docker compose --env-file .env.dev -f docker-compose.dev.yml --profile full up -d --build` | **`docker`** (automatic) | Containers |
| **Prod local** | `docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build` | **`prod`** (automatic) | Containers |
| **Prod registry** | `docker compose --env-file .env.prod -f docker-compose.prod.yml up -d` | **`prod`** (automatic) | Containers |

### Maven quick reference (hybrid dev only)

```bash
# user-service
cd userservice && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# activity-service
cd activityservice && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# ai-service
cd aiservice && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```
