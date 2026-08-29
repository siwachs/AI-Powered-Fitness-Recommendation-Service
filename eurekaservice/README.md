# Eureka Discovery Server (`eurekaservice`)

Spring Cloud Netflix Eureka service registry that manages dynamic service registration and discovery for all microservices in the fitness ecosystem.

---

## Overview

The Eureka Server enables decoupled communication between microservices. Services register their network locations at startup and resolve downstream endpoints using virtual service names (for example `lb://user-service` or `http://USER-SERVICE`).

```
                               +---------------------------------+
                               |     Eureka Discovery Server     |
                               |          (Port: 8761)           |
                               +----------------+----------------+
                                                |
                 +------------------------------+------------------------------+
                 |                              |                              |
                 v                              v                              v
        +------------------+          +-------------------+          +-------------------+
        |  GATEWAY-SERVICE |          |   USER-SERVICE    |          | ACTIVITY-SERVICE  |
        |   (Port: 8080)   |          |   (Port: 8081)    |          |   (Port: 8082)    |
        +------------------+          +-------------------+          +-------------------+
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
| `GATEWAY-SERVICE` | `8080` | Client entry point (fetches registry, does not register itself) |

---

## Verification and Health Check

### 1. Web Dashboard
Open `http://localhost:8761` in a browser to view active instances, uptime, and renewal status.

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
