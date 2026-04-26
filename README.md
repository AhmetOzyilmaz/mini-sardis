# mini-sardis — Subscription Management System

> A distributed subscription management system built with **Spring Boot 4**, **Apache Kafka**, and **Hexagonal Architecture** — designed for fault tolerance, idempotency, and production readiness.

![Java](https://img.shields.io/badge/Java-17-blue?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.5-brightgreen?logo=springboot)
![Apache Kafka](https://img.shields.io/badge/Apache_Kafka-3.x-black?logo=apachekafka)
![Gradle](https://img.shields.io/badge/Gradle-Multi--Module-02303A?logo=gradle)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker)
![License](https://img.shields.io/badge/License-MIT-yellow)

---

## Overview

Users can start, renew, and cancel subscriptions with payments processed asynchronously via an external provider. The system guarantees consistency through **Saga Choreography** and the **Transactional Outbox Pattern** — even when downstream services are temporarily unavailable.

| Capability | Detail |
|---|---|
| Async payment | `202 Accepted` on creation; state updated via Kafka event |
| Payment gate | Subscription never activates before payment succeeds |
| Auto-renewal | Scheduler runs daily at 09:00; exponential backoff on failure |
| Fault tolerance | Outbox pattern + circuit breaker + retry (Resilience4j) |
| Idempotency | Webhook dedup via `idempotencyKey`; Kafka at-least-once safe |
| Security | JWT Bearer auth, HMAC-SHA256 webhook signature, no PII in logs |
| Promo codes | Percentage / fixed discounts, usage limits, date ranges, plan restrictions |

---

## Services

| Service | Port | Responsibility |
|---|---|---|
| `subscription-service` | `8080` | Subscription lifecycle, auth (JWT), plans, promo codes, renewal scheduler |
| `payment-service` | `8081` | Payment processing, webhook handling, idempotency, circuit breaker |
| `notification-service` | `8082` | Kafka consumer, mock email/SMS dispatch, notification log |
| `sardis-common` | — | Shared library: global exception handler, logging config |

---

## Architecture

### System Context

```mermaid
graph TD
    User["User\nBrowser / Mobile"]
    Admin["Admin"]
    System["Subscription Management System\nSpring Boot Microservices"]
    Provider["Payment Provider\nMock"]
    Notif["Email / SMS Provider\nMock — Console Log"]
    Scheduler["Renewal Scheduler\nSpring @Scheduled"]

    User -->|"Start / Cancel / View subscription"| System
    Admin -->|"Plan management / promo codes"| System
    System -->|"Payment charge request"| Provider
    Provider -->|"Webhook result"| System
    System -->|"Notification dispatch"| Notif
    Scheduler -->|"Trigger monthly renewals"| System
```

### Microservices & Infrastructure

```mermaid
graph TD
    Client[Client App]
    PayProvider[Payment Provider Mock]

    subgraph DockerNetwork["Docker Network — sardis-network"]
        SubSvc["Subscription Service\n:8080 · Spring Boot · JPA"]
        PaySvc["Payment Service\n:8081 · Spring Boot"]
        NotifSvc["Notification Service\n:8082 · Spring Boot"]

        SubDB[("Subscription DB\nH2 / PostgreSQL")]
        PayDB[("Payment DB\nH2 / PostgreSQL")]
        NotifDB[("Notification DB\nH2 / PostgreSQL")]

        Kafka["Apache Kafka :9092"]
        KafkaUI["Kafka UI :9000"]
    end

    Client -->|":8080"| SubSvc
    Client -->|":8081"| PaySvc
    SubSvc <-->|JPA| SubDB
    PaySvc <-->|JPA| PayDB
    NotifSvc <-->|JPA| NotifDB
    SubSvc <-->|"pub/sub"| Kafka
    PaySvc <-->|"pub/sub"| Kafka
    NotifSvc -->|"consume"| Kafka
    KafkaUI --> Kafka
    PaySvc --> PayProvider
    PayProvider -->|webhook| PaySvc
```

### Subscription State Machine

```mermaid
stateDiagram-v2
    [*] --> PENDING : createSubscription()

    PENDING --> ACTIVE    : payment.completed.v1
    PENDING --> CANCELLED : payment.failed.v1

    ACTIVE --> CANCELLED  : userCancel() · DELETE /subscriptions/{id}
    ACTIVE --> SUSPENDED  : renewalFailed · max 3 retries exceeded

    SUSPENDED --> ACTIVE    : payment.completed.v1 (RENEWAL)
    SUSPENDED --> CANCELLED : adminAction or user request

    CANCELLED --> [*]
```

### Subscription Creation — Saga Sequence

```mermaid
sequenceDiagram
    participant U  as User
    participant SS as Subscription Service
    participant DB as Subscription DB
    participant OP as Outbox Poller
    participant K  as Kafka
    participant PS as Payment Service
    participant EP as Ext. Provider (Mock)
    participant NS as Notification Service

    U->>SS: POST /api/v1/subscriptions
    Note over SS: JWT check · @Valid
    SS->>DB: BEGIN TX
    SS->>DB: INSERT subscriptions (PENDING)
    SS->>DB: INSERT outbox_events (subscription.created.v1)
    SS->>DB: COMMIT
    SS-->>U: 202 Accepted · status=PENDING

    loop Every 5 s
        OP->>DB: SELECT unprocessed outbox rows
        OP->>K: publish subscription.created.v1
        OP->>DB: UPDATE processed=true
    end

    PS->>K: consume subscription.created.v1
    PS->>PS: INSERT payments (PENDING)
    PS->>EP: charge(amount)

    EP->>PS: POST /api/v1/payments/webhook
    Note over PS: Verify HMAC-SHA256
    Note over PS: Idempotency key dedup

    alt Payment SUCCESS
        PS->>K: publish payment.completed.v1
        SS->>K: consume · UPDATE status=ACTIVE
        NS->>K: consume · MOCK EMAIL sent
    else Payment FAILED
        PS->>K: publish payment.failed.v1
        SS->>K: consume · UPDATE status=CANCELLED
        NS->>K: consume · MOCK EMAIL sent
    end
```

### Hexagonal Architecture (per service)

```mermaid
graph LR
    subgraph IN["Inbound Adapters"]
        REST["REST Controller"]
        KafkaIn["Kafka Consumer"]
    end

    subgraph APP["Application Layer"]
        InPort["Inbound Ports\n(Use Case Interfaces)"]
        UC["Use Case Implementations"]
        OutPort["Outbound Ports\n(Repository / Event / Payment)"]
    end

    subgraph DOM["Domain"]
        Entity["Entities"]
        VO["Value Objects"]
        DE["Domain Events"]
    end

    subgraph OUT["Outbound Adapters"]
        JPA["JPA Repository"]
        KafkaOut["Kafka Publisher"]
        MockPay["Mock Payment Client"]
    end

    REST --> InPort
    KafkaIn --> InPort
    InPort --> UC
    UC --> Entity
    UC --> OutPort
    OutPort --> JPA
    OutPort --> KafkaOut
    OutPort --> MockPay
```

### Database Schema

```mermaid
erDiagram
    USERS ||--o{ SUBSCRIPTIONS : "has"
    SUBSCRIPTION_PLANS ||--o{ SUBSCRIPTIONS : "uses"
    SUBSCRIPTIONS ||--o{ PAYMENTS : "generates"
    SUBSCRIPTIONS ||--o{ OUTBOX_EVENTS : "produces"
    USERS ||--o{ NOTIFICATION_LOGS : "receives"
    PROMO_CODES }o--o{ SUBSCRIPTIONS : "applied to"

    USERS {
        uuid id PK
        varchar email UK
        varchar full_name
        varchar phone_number
        varchar role
        boolean active
        timestamp created_at
    }

    SUBSCRIPTION_PLANS {
        uuid id PK
        varchar name UK
        decimal price
        varchar currency
        int duration_days
        int trial_days
        boolean active
    }

    SUBSCRIPTIONS {
        uuid id PK
        uuid user_id FK
        uuid plan_id FK
        uuid promo_code_id FK
        varchar status
        date start_date
        date next_renewal_date
        decimal amount
        decimal discount_amount
        decimal final_amount
        int version
    }

    PAYMENTS {
        uuid id PK
        uuid subscription_id FK
        varchar idempotency_key UK
        decimal amount
        varchar currency
        varchar status
        varchar type
        varchar payment_method
        int retry_count
        timestamp created_at
    }

    OUTBOX_EVENTS {
        uuid id PK
        uuid aggregate_id
        varchar event_type
        text payload
        boolean processed
        timestamp created_at
    }

    PROMO_CODES {
        uuid id PK
        varchar code UK
        varchar discount_type
        decimal discount_value
        int max_uses
        int current_uses
        boolean active
        timestamp valid_from
        timestamp valid_to
    }

    NOTIFICATION_LOGS {
        uuid id PK
        uuid user_id FK
        varchar channel
        varchar subject
        boolean success
        timestamp sent_at
    }
```

---

## Kafka Topics

| Topic | Publisher | Consumer(s) |
|---|---|---|
| `subscription.created.v1` | Subscription Service | Payment Service |
| `subscription.activated.v1` | Subscription Service | Notification Service |
| `subscription.cancelled.v1` | Subscription Service | Notification Service |
| `subscription.failed.v1` | Subscription Service | Notification Service |
| `subscription.renewed.v1` | Subscription Service | Notification Service |
| `subscription.suspended.v1` | Subscription Service | Notification Service |
| `payment.completed.v1` | Payment Service | Subscription Service, Notification Service |
| `payment.failed.v1` | Payment Service | Subscription Service, Notification Service |
| `renewal.requested.v1` | Subscription Scheduler | Payment Service |

---

## Quick Start

```bash
# Full stack via Docker Compose
docker-compose up --build
```

```bash
# Individual services — dev profile (H2 in-memory, mock providers)
./gradlew :subscription-service:bootRun  --args='--spring.profiles.active=dev'
./gradlew :payment-service:bootRun       --args='--spring.profiles.active=dev'
./gradlew :notification-service:bootRun  --args='--spring.profiles.active=dev'
```

```bash
# Run all tests
./gradlew test

# Build without tests
./gradlew build -x test
```

### Developer URLs

| URL | Description |
|---|---|
| `http://localhost:8080/swagger-ui.html` | Swagger UI — Subscription Service |
| `http://localhost:8081/swagger-ui.html` | Swagger UI — Payment Service |
| `http://localhost:8082/swagger-ui.html` | Swagger UI — Notification Service |
| `http://localhost:8080/h2-console` | H2 DB Console (dev profile) |
| `http://localhost:9000` | Kafka UI |
| `http://localhost:8080/actuator/health` | Health check |

---

## Tech Stack

| Category | Technology | Rationale |
|---|---|---|
| Framework | Spring Boot 4.0.5 | Production-ready ecosystem |
| Language | Java 17 | LTS — records, text blocks, pattern matching |
| Database | H2 (dev/test) · PostgreSQL (prod) | Fast local dev + production parity |
| Migrations | Flyway | Versioned, deterministic schema |
| Messaging | Apache Kafka | Fan-out, event replay, at-least-once delivery |
| Architecture | Hexagonal (Ports & Adapters) | Domain isolated; adapters swappable |
| Distributed TX | Saga Choreography + Outbox | No dual-write problem |
| Mapper | MapStruct | Compile-time, zero reflection |
| Resilience | Resilience4j | Circuit breaker + retry + backoff |
| Security | Spring Security + jjwt | JWT Bearer, HMAC-SHA256 webhook |
| API Docs | SpringDoc OpenAPI 3.0.2 | Auto-generated Swagger UI |
| Shared Library | `sardis-common` | Exception handlers, logback config |
| Testing | JUnit 5 + Mockito + EmbeddedKafka | Unit → component coverage |
| Build | Gradle multi-module | Single root build for all services |
| Container | Docker + Docker Compose | Reproducible environment |

---

## Key Design Decisions

| Decision | Why |
|---|---|
| **Outbox Pattern** | DB state + Kafka event committed atomically — eliminates dual-write |
| **Saga Choreography** | Three services react to events; no central orchestrator needed |
| **`@Version` on Subscription** | Optimistic locking prevents concurrent cancel + renewal race |
| **No `setStatus()`** | State machine enforced inside domain entity; invalid transitions throw |
| **Hexagonal per service** | Domain is 100% unit-testable; adapters (JPA, Kafka, HTTP) are swappable |
| **Mock providers** | `NotificationPort` / `PaymentPort` — swap to SendGrid/Stripe without touching use cases |
| **H2 → PostgreSQL** | Same Flyway migrations run in both; only datasource config changes |
| **`sardis-common`** | Shared `GlobalExceptionHandlerBase` + `logback-spring.xml` loaded by all services |

---

## Documentation

### Project & Architecture

| Document | Description |
|---|---|
| [Subscription Service Overview](subscription-service/README.md) | Full system design with all Mermaid diagrams, state machine, saga sequence, ER diagram |
| [Architecture Decision Records](docs/architecture/ADR.md) | 10 ADRs — microservices, hexagonal, Kafka, outbox, idempotency, JWT, MapStruct, and more |

### Service Design

| Document | Description |
|---|---|
| [Subscription Service Design](subscription-service/docs/README.md) | Hexagonal layout, domain model, state machine, Flyway migrations, Kafka topics |
| [Payment Service Design](payment-service/docs/README.md) | Payment flow, webhook signature verification, circuit breaker, retry logic |
| [Notification Service Design](notification-service/docs/README.md) | Kafka consumer setup, mock email/SMS adapters, notification log |

### API & Testing

| Document | Description |
|---|---|
| [API cURL Reference](docs/api-curl-reference.md) | Ready-to-run curl commands for all 13 endpoints; Postman import guide; HMAC signature helper scripts; end-to-end flow |

### Security & Deployment

| Document | Description |
|---|---|
| [Security Design](docs/security/README.md) | JWT authentication flow, role-based access control, webhook HMAC-SHA256 verification |
| [Deployment Guide](docs/deployment/README.md) | Docker Compose configuration, environment variables, demo walkthrough, troubleshooting |

---

## Project Structure

```
mini-sardis/
├── sardis-common/                   # Shared library (exception handler, logback config)
├── subscription-service/            # Port 8080 — lifecycle, auth, plans, promo codes
│   ├── src/main/java/.../
│   │   ├── domain/                  # Pure Java — entities, value objects, events
│   │   ├── application/             # Use cases, port interfaces
│   │   └── infrastructure/          # REST, Kafka, JPA, security, Flyway
│   └── src/test/java/.../
│       ├── application/service/     # Unit tests (Mockito, no Spring)
│       └── infrastructure/adapter/  # Component tests (SpringBootTest + EmbeddedKafka)
├── payment-service/                 # Port 8081 — payments, webhook, idempotency
├── notification-service/            # Port 8082 — Kafka consumer, email/SMS log
├── docs/
│   ├── api-curl-reference.md        # curl / Postman reference for all endpoints
│   ├── architecture/ADR.md          # Architecture Decision Records
│   ├── security/README.md           # Security design
│   └── deployment/README.md         # Deployment guide
└── docker-compose.yml
```

---

> **Author:** Ahmet Özyılmaz &nbsp;·&nbsp; mini-sardis &nbsp;·&nbsp; Turkcell Case Study
