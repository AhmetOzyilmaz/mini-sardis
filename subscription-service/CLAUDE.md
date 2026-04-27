# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Run the application (dev profile — H2 in-memory, mock providers)
./gradlew bootRun --args='--spring.profiles.active=dev'

# Build fat JAR
./gradlew bootJar

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.mini.sardis.application.service.CreateSubscriptionUseCaseTest"

# Run tests matching a pattern
./gradlew test --tests "*RealWorld*"

# Full stack via Docker Compose
docker-compose up --build

# Build without tests
./gradlew build -x test
```

## Architecture

This project is a subscription management system built with **Hexagonal Architecture (Ports & Adapters)** inside each microservice, communicating via **Apache Kafka**. Read `claude/prd.md` for the full PRD and `docs/architecture/ADR.md` for all architectural decisions.

### Three microservices (currently one Spring Boot module; packages follow service boundaries)

| Service | Port | Owns |
|---------|------|------|
| Subscription Service | :8080 | Subscription lifecycle, state machine, renewal scheduler, Outbox |
| Payment Service | :8081 | Payment charge, webhook handler, idempotency, circuit breaker |
| Notification Service | :8082 | Kafka event consumer, mock email/SMS log, notification history |

### Hexagonal package layout (enforced per service)

```
com.mini.sardis.
├── domain/           ← Pure Java. Zero Spring/JPA annotations here.
│   ├── entity/       ← Subscription, Payment (no @Entity — that lives in infrastructure)
│   ├── value/        ← SubscriptionStatus, Money, PaymentType (immutable)
│   ├── event/        ← SubscriptionCreatedEvent, PaymentCompletedEvent
│   └── service/      ← Domain rules (no Spring deps)
├── application/
│   ├── port/in/      ← Use case interfaces (CreateSubscriptionUseCase, etc.)
│   ├── port/out/     ← Repository/event ports (SubscriptionRepositoryPort, etc.)
│   └── service/      ← Use case implementations (one class per operation)
└── infrastructure/
    ├── adapter/in/   ← REST controllers, Kafka consumers, MapStruct request mappers
    ├── adapter/out/  ← JPA entities + repos, Kafka publishers, mock payment client, MapStruct JPA mappers
    ├── config/       ← SecurityConfig, KafkaConfig, CacheConfig, OpenAPIConfig
    └── persistence/  ← Flyway migrations (db/migration/V1__*.sql … V8__seed_demo_data.sql)
```

**Rule:** Domain classes must not import anything from `infrastructure` or Spring. Use case classes depend only on port interfaces, never on adapter implementations.

### Key cross-cutting patterns

**Outbox Pattern** — Every Kafka event is written as an `outbox_events` DB row in the same transaction as the business entity. A `@Scheduled` poller (every 5s) reads unprocessed rows, publishes to Kafka, then marks them processed. This eliminates the dual-write problem.

**Saga Choreography** — Subscription creation is a distributed transaction across three services. Each service reacts to Kafka events and triggers its local transaction. Compensating transaction: if `payment.failed.v1` arrives, `Subscription` transitions PENDING → CANCELLED.

**Subscription State Machine** — Status changes happen only through behavior methods on the `Subscription` entity (`activate()`, `cancel()`, `suspend()`). There is no public `setStatus()`. Invalid transitions throw `InvalidStateTransitionException`. The entity holds an `int version` field for optimistic locking (`@Version`).

**Idempotency** — Payment webhooks carry an `idempotencyKey`. Before processing, the handler checks for an existing `payments` row with that key (backed by a `UNIQUE` DB constraint). Duplicate webhooks return 200 without reprocessing.

**MapStruct mappers** live in `infrastructure/adapter/*/mapper/`. Three kinds: request DTO → domain command (inbound), domain → JPA entity (outbound), domain → Kafka event DTO (outbound). Domain objects never know about DTOs or JPA entities.

### Dependencies in `build.gradle` (current)

Spring Boot 4.0.5 · Java 21 toolchain · Spring Data JPA · Spring WebMVC · Spring WebFlux · Spring Security · Spring Data REST · Flyway · SpringDoc OpenAPI 3.0.2 · Lombok · H2

**Not yet added — must be included when implementing:**
- `spring-kafka` + `spring-kafka-test` (EmbeddedKafka for tests)
- `mapstruct` + `mapstruct-processor`
- `resilience4j-spring-boot3` (retry + circuit breaker)
- `jjwt` or `spring-security-oauth2-resource-server` (JWT)

### Database

Flyway migrations in `src/main/resources/db/migration/`. Naming: `V{n}__{description}.sql`. H2 is the dev database. Migrations must be PostgreSQL-compatible (avoid H2-only syntax). H2 does not support partial indexes (`WHERE` clause in `CREATE INDEX`) — use compound indexes in migrations; document PostgreSQL-specific variants in comments.

### Testing conventions

- **Unit tests** (`domain/`, `application/service/`) — plain JUnit 5 + Mockito, no Spring context.
- **Component tests** (`infrastructure/adapter/in/`) — `@SpringBootTest` + `MockMvc` + H2 + `@EmbeddedKafka`.
- **Repository tests** (`infrastructure/adapter/out/jpa/`) — `@DataJpaTest` + H2.
- Real-world failure scenarios (concurrent updates, duplicate Kafka delivery, circuit breaker open) belong in `*RealWorldFailureTest` classes.
- Cache is disabled in tests via `NullCacheManager` — never rely on cache state in assertions.

### Security model

- **JWT Bearer** for all user-facing endpoints. Roles: `ROLE_USER`, `ROLE_ADMIN`.
- **HMAC-SHA256** on `POST /api/v1/payments/webhook` via `X-Signature` header — verified before idempotency check.
- **Public endpoints**: `GET /api/v1/plans`, `POST /api/v1/auth/login`, `POST /api/v1/auth/register`.
- Secrets (JWT secret, webhook secret) come from environment variables — never hardcoded.

### Kafka topic naming

`{service}.{domain-event}.{version}` — e.g. `subscription.created.v1`, `payment.completed.v1`. All topics versioned; consumers stay on old version while new version rolls out.

### Documentation

- `claude/prd.md` — full bilingual PRD with all Mermaid diagrams
- `docs/architecture/ADR.md` — 10 ADRs explaining every major decision
- `docs/{subscription,payment,notification}-service/README.md` — per-service hexagonal design
- `docs/deployment/README.md` — Docker Compose config, demo curl walkthrough, troubleshooting
- `claude/backend-rules.md` — coding rules for input validation, error handling, security, testing
