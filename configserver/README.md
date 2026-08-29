# Config Server (`configserver`)

Spring Cloud Config Server providing centralized, externalized configuration management across all microservices in the **AI-Powered Fitness Recommendation Platform**.

---

## Overview

The Config Server operates in `native` profile mode, reading configuration files from its local classpath (`classpath:/config/shared` and `classpath:/config/{application}`). Microservices fetch their active configurations at startup based on their `spring.application.name` and active profile (`dev`, `docker`, `prod`).

```
                              ┌─────────────────────────────┐
                              │  Spring Cloud Config Server │
                              │         (Port: 8888)        │
                              └──────────────┬──────────────┘
                                             │
               ┌─────────────────────────────┼─────────────────────────────┐
               │                             │                             │
               ▼                             ▼                             ▼
       ┌───────────────┐             ┌───────────────┐             ┌───────────────┐
       │Gateway Service│             │  User Service │             │Activity / AI  │
       │  (Port: 8080) │             │  (Port: 8081) │             │ (8082 / 8083) │
       └───────────────┘             └───────────────┘             └───────────────┘
```

---

## Specifications

| Property | Details |
|---|---|
| **Port** | `8888` |
| **Spring Boot Version** | `3.5.x` |
| **Spring Cloud Version** | `2025.x` |
| **Active Backend** | `native` (Classpath file storage) |
| **Search Locations** | `classpath:/config/shared`, `classpath:/config/{application}` |

---

## Configuration Directory Structure

```
configserver/src/main/resources/config/
├── shared/                         # Common configuration loaded by all services
│   ├── application.yml             # Common actuator & health probes
│   ├── application-dev.yml         # Dev Eureka URL (localhost:8761)
│   ├── application-docker.yml      # Docker network Eureka & Config server URLs
│   └── application-prod.yml        # Production eureka & config server settings
├── gateway-service/
│   └── application.yaml            # Port 8080, Keycloak OAuth2 JWT issuer, Route definitions
├── user-service/
│   ├── application.yml             # Port 8081
│   ├── application-dev.yml         # PostgreSQL on localhost:5432
│   ├── application-docker.yml      # PostgreSQL on postgres:5432
│   └── application-prod.yml        # Production Postgres with 'validate' DDL
├── activity-service/
│   ├── application.yml             # Port 8082, RabbitMQ exchange/queue/key names
│   ├── application-dev.yml         # MongoDB & RabbitMQ on localhost
│   ├── application-docker.yml      # MongoDB & RabbitMQ on Docker network
│   └── application-prod.yml        # Production MongoDB URI & RabbitMQ credentials
└── ai-service/
    ├── application.yml             # Port 8083, RabbitMQ bindings, Gemini API properties
    ├── application-dev.yml         # MongoDB & RabbitMQ on localhost
    ├── application-docker.yml      # MongoDB & RabbitMQ on Docker network
    └── application-prod.yml        # Production MongoDB & Gemini credentials
```

---

## Config Server Endpoints

Microservices and developers can inspect the active configuration served for any service by querying:

### 1. Fetch Service Configuration by Profile
```http
GET http://localhost:8888/{application}/{profile}
```

**Examples:**
```bash
# Get Gateway configuration for docker profile
curl http://localhost:8888/gateway-service/docker

# Get User Service configuration for dev profile
curl http://localhost:8888/user-service/dev

# Get Activity Service configuration for prod profile
curl http://localhost:8888/activity-service/prod

# Get AI Service configuration for dev profile
curl http://localhost:8888/ai-service/dev
```

### 2. Health & Readiness
```bash
curl http://localhost:8888/actuator/health
```

---

## Running the Service

### Run with Maven
```bash
cd configserver
./mvnw spring-boot:run
```

### Run with Docker
```bash
docker build -t fitness-config-server .
docker run -p 8888:8888 fitness-config-server
```
