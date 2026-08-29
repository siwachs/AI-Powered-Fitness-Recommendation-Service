# Eureka Discovery Server (`eurekaservice`)

Spring Cloud Netflix Eureka service registry that manages dynamic service registration and discovery for all microservices in the fitness ecosystem.

---

## Overview

The Eureka Server enables decoupled, location-transparent communication between microservices. Instead of hardcoding hostnames and IP addresses, services register their network locations at startup and resolve downstream endpoints using virtual service names (e.g., `lb://user-service`, `http://USER-SERVICE`).

```
                               ┌─────────────────────────────────┐
                               │     Eureka Discovery Server     │
                               │          (Port: 8761)           │
                               └────────────────┬────────────────┘
                                                │
                 ┌──────────────────────────────┼──────────────────────────────┐
                 ▼                              ▼                              ▼
        ┌──────────────────┐          ┌───────────────────┐          ┌───────────────────┐
        │  GATEWAY-SERVICE │          │   USER-SERVICE    │          │ ACTIVITY-SERVICE  │
        │   (Port: 8080)   │          │   (Port: 8081)    │          │   (Port: 8082)    │
        └──────────────────┘          └───────────────────┘          └───────────────────┘
```

---

## Specifications

| Property | Value |
|---|---|
| **Port** | `8761` |
| **Web Dashboard** | `http://localhost:8761` |
| **Registration URL** | `http://localhost:8761/eureka/` |
| **Self-Registration** | `false` |
| **Fetch-Registry** | `false` |

---

## Registered Service Instances

When all microservices are running, Eureka maintains heartbeats and registers the following service IDs:

| Service Name in Eureka | Typical Port | Consumers |
|---|---|---|
| `USER-SERVICE` | `8081` | `GATEWAY-SERVICE`, `ACTIVITY-SERVICE` |
| `ACTIVITY-SERVICE` | `8082` | `GATEWAY-SERVICE` |
| `AI-SERVICE` | `8083` | `GATEWAY-SERVICE` |
| `GATEWAY-SERVICE` | `8080` | *(Client Entry Point - does not self-register, fetches registry)* |

---

## Verification & Health Check

### 1. Web Dashboard
Navigate to `http://localhost:8761` in your browser to view active instances, memory usage, uptime, and renewal thresholds.

### 2. Actuator Health
```bash
curl http://localhost:8761/actuator/health
```

---

## Running the Service

### Run with Maven
```bash
cd eurekaservice
./mvnw spring-boot:run
```

### Run with Docker
```bash
docker build -t fitness-eureka-server .
docker run -p 8761:8761 fitness-eureka-server
```
