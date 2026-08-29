# API Gateway Service (`gatewayservice`)

Non-blocking, reactive edge gateway built with Spring Cloud Gateway (WebFlux) and Spring Security OAuth2 Resource Server. Acts as the unified API entry point, token validator, and automated identity synchronizer.

---

## Architectural Role

```
   Client (Web / Mobile / Postman)
                 |
                 | 1. Request with Bearer JWT
                 v
     +--------------------------------------------------------------+
     |                      GATEWAY SERVICE                         |
     |                        (Port: 8080)                          |
     |                                                              |
     |   [1. OAuth2 JWT Validation via Keycloak]                    |
     |   [2. KeycloakUserSyncFilter]                                |
     |       |-- Extract Keycloak UUID (sub), Email, Name           |
     |       |-- Check User existence via lb://user-service         |
     |       |-- Auto-register user in user-service if not found    |
     |       `-- Mutate request header -> Add "X-User-ID: <UUID>"   |
     |   [3. Route to Target Downstream Microservice]               |
     +---------------+------------------------------+---------------+
                     |                              |
        Path: /api/v1/users/**         Path: /api/v1/activities/**
                     |                              |
                     v                              v
             +---------------+              +---------------+
             |  USER SERVICE |              |ACTIVITY SERVIC|
             | (Port: 8081)  |              | (Port: 8082)  |
             +---------------+              +---------------+
```

---

## Specifications

| Property | Details |
|---|---|
| **Port** | `8080` |
| **Framework** | Spring Cloud Gateway (Reactive WebFlux) |
| **Authentication** | OAuth2 Resource Server (JWT verification with Keycloak) |
| **Identity Issuer** | `${KEYCLOAK_ISSUER_URI}` (e.g. `http://localhost:8180/realms/fitness-oauth2`) |
| **Service Discovery** | Fetches registry from Eureka Server (`http://localhost:8761/eureka/`) |
| **Load Balancing** | Spring Cloud LoadBalancer (`lb://<SERVICE_NAME>`) |

---

## Routing Configuration

Routes are configured in Config Server (`config/gateway-service/application.yaml`):

| Route ID | Path Predicate | Target URI | Purpose |
|---|---|---|---|
| `user-service` | `/api/v1/users/**` | `lb://user-service` | User profile & validation |
| `activity-service` | `/api/v1/activities/**` | `lb://activity-service` | Fitness activity tracking |
| `ai-service` | `/api/v1/recommendations/**` | `lb://ai-service` | AI workout recommendations |

---

## KeycloakUserSyncFilter Mechanism

The gateway implements a custom reactive WebFilter (`KeycloakUserSyncFilter`):
1. **Token Parsing**: Parses the incoming `Authorization: Bearer <token>` using `nimbus-jose-jwt`.
2. **Claims Extraction**: Extracts `sub` (Keycloak UUID), `email`, `given_name`, and `family_name`.
3. **User Validation**: Calls `userService.validateUser(keycloakId)` (`GET /api/v1/users/{keycloakId}/validate`) via load-balanced WebClient.
4. **Just-In-Time Registration**: If the user does not exist in `userservice`, invokes `POST /api/v1/users/register`.
5. **Header Propagation**: Adds `X-User-ID: <keycloak-uuid>` to the downstream request so downstream services (`activityservice`) receive the authenticated user ID directly.

---

## Environment Variables

| Variable | Description | Example |
|---|---|---|
| `CONFIG_SERVER_URL` | URL of the central Config Server | `http://config-server:8888` / `http://localhost:8888` |
| `KEYCLOAK_ISSUER_URI` | OpenID Connect JWT issuer URI | `http://keycloak:8080/realms/fitness-oauth2` |
| `EUREKA_URL` | Eureka Service Registry endpoint | `http://eureka-server:8761/eureka/` |

---

## How to Run

### Hybrid Mode (Local Host)
```bash
cd gatewayservice
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Docker Mode
```bash
docker build -t fitness-gateway-service .
docker run -p 8080:8080 \
  -e CONFIG_SERVER_URL=http://host.docker.internal:8888 \
  -e KEYCLOAK_ISSUER_URI=http://host.docker.internal:8180/realms/fitness-oauth2 \
  fitness-gateway-service
```
