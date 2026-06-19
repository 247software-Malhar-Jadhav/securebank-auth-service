# securebank-auth-service

The **identity microservice** of the SecureBank platform. It is the single authority
that issues and verifies JSON Web Tokens (JWTs). It speaks two protocols:

- **REST** (port **8081**) — browser-facing auth flows, reached via the gateway under
  `/api/auth/*`.
- **gRPC** (port **9091**) — for the gateway and other services to *verify* tokens and
  look up users without ever holding the signing key.

Database: PostgreSQL, schema/db **`auth`** (database-per-service). Java 21 with virtual
threads, Spring Boot 3.3.

> Conforms to `../MICROSERVICES_SPEC.md` (ports, package `com.securebank.auth`,
> conventions).

---

## What it does

- **Register / Login / Refresh** with BCrypt-hashed passwords.
- **Account lockout**: 5 consecutive failed logins lock the account for 15 minutes.
- **JWT issuance**: HS256, issuer `securebank`, embedding `roles` and the user's
  `preferred_locale`. Separate access (15 min) and refresh (7 day) tokens.
- **gRPC `AuthService`**: `ValidateToken` (parse + verify a JWT, return decoded claims)
  and `GetUser` (basic user lookup by id). This is the *trusted internal authority*
  pattern — see `docs/auth-service.md`.
- **RFC-7807** problem responses, with messages localised to **en / hi / mr** via
  `MessageSource` (real Devanagari in the Hindi/Marathi bundles).

## gRPC contracts

The proto files (`auth.proto`, `common.proto`) are **vendored** into `src/main/proto/`
(copied from `securebank-contracts`, per spec §2). Java + gRPC stubs are generated
locally at build time by `protobuf-maven-plugin` — there is **no dependency on a remote
contracts jar**, so the repo builds standalone.

## Run locally

Prerequisites: JDK 21, Maven, a PostgreSQL with a database named `auth`.

```bash
# 1. Start Postgres (example with Docker)
docker run --rm -d --name sb-pg -p 5432:5432 \
  -e POSTGRES_DB=auth -e POSTGRES_USER=auth -e POSTGRES_PASSWORD=auth postgres:16

# 2. Provide a JWT secret (>= 32 bytes) and run
export SECUREBANK_JWT_SECRET="local-dev-secret-change-me-0123456789-abcdef"
mvn spring-boot:run
```

Flyway runs `V1__init.sql` (schema) and `V2__seed.sql` (seed users) on startup.

Seeded accounts (password is `Password123!` for both):

| username | password       | role     |
|----------|----------------|----------|
| admin    | `Password123!` | ADMIN    |
| jsmith   | `Password123!` | CUSTOMER |

### Docker

```bash
docker build -t securebank/auth-service .
docker run --rm -p 8081:8081 -p 9091:9091 \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e SECUREBANK_JWT_SECRET="..." \
  securebank/auth-service
```

### Kubernetes

```bash
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secret.yaml      # edit the placeholders first!
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
```

## REST endpoints

All under `/api/auth` (the gateway forwards these). Bodies are JSON.

| Method | Path        | Body                                            | Returns                              |
|--------|-------------|-------------------------------------------------|--------------------------------------|
| POST   | `/register` | `{username,email,password,preferredLocale?}`    | `201` `UserResponse`                 |
| POST   | `/login`    | `{username,password}`                           | `200` `{accessToken,refreshToken,…}` |
| POST   | `/refresh`  | `{refreshToken}`                                | `200` `{accessToken,refreshToken,…}` |

Example:

```bash
curl -s localhost:8081/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"jsmith","password":"Password123!"}'
```

Errors are RFC-7807 `application/problem+json`. Send `Accept-Language: hi` (or `mr`) to
get localised messages.

OpenAPI/Swagger UI: `http://localhost:8081/swagger-ui.html`.

## gRPC endpoints (port 9091)

`securebank.auth.v1.AuthService`:

- `ValidateToken(TokenRequest{access_token}) → TokenClaims{valid,user_id,username,roles,preferred_locale}`
- `GetUser(UserIdRequest{user_id}) → UserInfo{user_id,username,email,roles,enabled}`

An invalid/expired token returns `valid=false` (not a gRPC error); the caller rejects
the request.

## Observability

- Health: `GET /actuator/health` (+ `/health/liveness`, `/health/readiness`)
- Metrics: `GET /actuator/prometheus`

## Tech / patterns

Spring Web · Spring Security 6 (BCrypt) · Spring Data JPA · Flyway · jjwt ·
`net.devh:grpc-spring-boot-starter` · springdoc · micrometer-prometheus.

Patterns used: layered architecture (controller → service → repository), DTOs distinct
from entities, static-factory exceptions mapped to RFC-7807, externalised configuration
(`JwtProperties`), Adapter (gRPC layer), and the *trusted internal authority* token
model. See `docs/auth-service.md`.
