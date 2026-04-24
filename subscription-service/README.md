# Subscription Management System

> A distributed subscription management system built with Spring Boot, Apache Kafka, and Hexagonal Architecture — designed for fault tolerance, idempotency, and production readiness.

![Java](https://img.shields.io/badge/Java-17-blue?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0-brightgreen?logo=springboot)
![Apache Kafka](https://img.shields.io/badge/Apache_Kafka-3.x-black?logo=apachekafka)
![Docker](https://img.shields.io/badge/Docker-Compose-blue?logo=docker)
![License](https://img.shields.io/badge/License-MIT-yellow)

---

## Overview

Users can start, renew, and cancel subscriptions with credit card payments processed asynchronously via an external provider. The system guarantees consistency through the **Saga Choreography** pattern and the **Transactional Outbox Pattern** — even when downstream services are temporarily unavailable.

| Capability | Detail |
|---|---|
| Async payment | HTTP 202 on creation; state updated via Kafka event |
| Payment gate | Subscription never activates before payment succeeds |
| Auto-renewal | Scheduler runs daily at 09:00; exponential backoff on failure |
| Fault tolerance | Outbox pattern + circuit breaker + retry (Resilience4j) |
| Idempotency | Webhook dedup via `idempotencyKey`; Kafka at-least-once safe |
| Security | JWT Bearer, HMAC-SHA256 webhook signature, no PII in logs |

---

## Architecture

### System Context

```mermaid
graph TD
    User["👤 User\nBrowser / Mobile"]
    Admin["👤 Admin"]
    System["🏢 Subscription Management System\nSpring Boot Microservices"]
    Provider["💳 Payment Provider\nMock"]
    Notif["📧 Email / SMS Provider\nMock — Console Log"]
    Scheduler["⏰ Renewal Scheduler\nSpring @Scheduled"]

    User -->|"Start / Cancel / View subscription"| System
    Admin -->|"Plan management / reports"| System
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
        PaySvc["Payment Service\n:8081 · Spring Boot · WebFlux"]
        NotifSvc["Notification Service\n:8082 · Spring Boot"]

        SubDB[("Subscription DB\nH2 / PostgreSQL")]
        PayDB[("Payment DB\nH2 / PostgreSQL")]
        NotifDB[("Notification DB\nH2 / PostgreSQL")]

        ZK["Zookeeper :2181"]
        Kafka["Apache Kafka :9092"]
        KafkaUI["Kafka UI :9000"]
    end

    Client -->|":8080"| SubSvc
    Client -->|":8081"| PaySvc
    SubSvc <-->|JPA| SubDB
    PaySvc <-->|JPA| PayDB
    NotifSvc <-->|JPA| NotifDB
    ZK --> Kafka
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

    PENDING --> ACTIVE : paymentSuccess\npayment.completed.v1
    PENDING --> CANCELLED : paymentFailed\npayment.failed.v1

    ACTIVE --> CANCELLED : userCancel()\nDELETE /subscriptions/{id}
    ACTIVE --> SUSPENDED : renewalFailed\nmax 3 retries exceeded

    SUSPENDED --> ACTIVE : renewalSuccess\npayment.completed.v1 RENEWAL
    SUSPENDED --> CANCELLED : adminAction\nor user request

    CANCELLED --> [*]
```

### Subscription Creation — Saga Sequence

```mermaid
sequenceDiagram
    participant U as User
    participant SS as Subscription Service
    participant DB as Subscription DB
    participant OP as Outbox Poller
    participant K as Kafka
    participant PS as Payment Service
    participant EP as Ext. Provider (Mock)
    participant NS as Notification Service

    U->>SS: POST /api/v1/subscriptions
    Note over SS: @Valid · JWT check
    SS->>DB: BEGIN TX
    SS->>DB: INSERT subscriptions (PENDING)
    SS->>DB: INSERT outbox_events (subscription.created.v1)
    SS->>DB: COMMIT
    SS-->>U: 202 Accepted · status=PENDING

    loop Every 5s
        OP->>DB: SELECT unprocessed WHERE processed=false
        OP->>K: publish subscription.created.v1
        OP->>DB: UPDATE processed=true
    end

    PS->>K: consume subscription.created.v1
    PS->>PS: INSERT payments (PENDING, INITIAL)
    PS->>EP: charge(cardToken, amount)

    EP->>PS: POST /api/v1/payments/webhook
    Note over PS: Verify HMAC-SHA256
    Note over PS: Check idempotencyKey (dedup)
    PS->>PS: UPDATE payments (SUCCESS/FAILED)

    alt Payment SUCCESS
        PS->>K: publish payment.completed.v1
        SS->>K: consume payment.completed.v1
        SS->>DB: UPDATE status=ACTIVE, nextRenewalDate
        SS->>DB: INSERT outbox (subscription.activated.v1)
        NS->>K: consume subscription.activated.v1
        NS->>NS: MOCK EMAIL — Subscription activated
        NS->>DB: INSERT notification_logs
    else Payment FAILED
        PS->>K: publish payment.failed.v1
        SS->>K: consume payment.failed.v1
        SS->>DB: UPDATE status=CANCELLED
        NS->>NS: MOCK EMAIL — Payment failed
        NS->>DB: INSERT notification_logs
    end
```

### Hexagonal Architecture (per Service)

```mermaid
graph LR
    subgraph IN["Infrastructure — Inbound Adapters"]
        REST["REST Controller\nSubscriptionController"]
        KafkaIn["Kafka Consumer\nPaymentEventListener"]
    end

    subgraph APP["Application Layer"]
        InPort["Inbound Ports\nCreateSubscriptionUseCase\nCancelSubscriptionUseCase"]
        UC["Use Case Implementations"]
        OutPortRepo["SubscriptionRepositoryPort"]
        OutPortEvent["EventPublisherPort"]
        OutPortPay["PaymentPort"]
    end

    subgraph DOM["Domain"]
        Entity["Entities\nSubscription · Payment"]
        VO["Value Objects\nSubscriptionStatus · Money"]
        DE["Domain Events\nSubscriptionCreatedEvent"]
    end

    subgraph OUT["Infrastructure — Outbound Adapters"]
        JPA["JpaSubscriptionRepository"]
        KafkaOut["KafkaEventPublisher"]
        MockPay["MockPaymentClient"]
        Mapper["MapStruct Mappers"]
    end

    REST --> InPort
    KafkaIn --> InPort
    InPort --> UC
    UC --> Entity
    UC --> OutPortRepo
    UC --> OutPortEvent
    UC --> OutPortPay
    OutPortRepo --> JPA
    OutPortEvent --> KafkaOut
    OutPortPay --> MockPay
    JPA <--> Mapper
    REST <--> Mapper
```

### Database ER Diagram

```mermaid
erDiagram
    USERS ||--o{ SUBSCRIPTIONS : "has"
    SUBSCRIPTION_PLANS ||--o{ SUBSCRIPTIONS : "defines"
    SUBSCRIPTIONS ||--o{ PAYMENTS : "generates"
    SUBSCRIPTIONS ||--o{ OUTBOX_EVENTS : "produces"
    USERS ||--o{ NOTIFICATION_LOGS : "receives"

    USERS {
        uuid id PK
        varchar email UK
        varchar full_name
        varchar phone_number
        boolean active
        timestamp created_at
    }

    SUBSCRIPTION_PLANS {
        uuid id PK
        varchar name UK
        decimal price
        int duration_days
        int trial_days
        boolean active
    }

    SUBSCRIPTIONS {
        uuid id PK
        uuid user_id FK
        uuid plan_id FK
        varchar status
        date start_date
        date next_renewal_date
        int version
    }

    PAYMENTS {
        uuid id PK
        uuid subscription_id FK
        varchar idempotency_key UK
        decimal amount
        varchar status
        varchar type
        int retry_count
    }

    OUTBOX_EVENTS {
        uuid id PK
        uuid aggregate_id
        varchar event_type
        text payload
        boolean processed
    }

    NOTIFICATION_LOGS {
        uuid id PK
        uuid user_id FK
        varchar channel
        boolean success
        timestamp sent_at
    }
```

---

## Tech Stack

| Category | Technology | Rationale |
|---|---|---|
| Framework | Spring Boot 4.x | Production-ready ecosystem |
| Language | Java 17 | LTS — records, sealed classes |
| Database | H2 (dev) / PostgreSQL (prod) | Fast local dev + production parity |
| Migrations | Flyway | Versioned, deterministic schema |
| Messaging | Apache Kafka | Fan-out, event replay, at-least-once |
| Architecture | Hexagonal (Ports & Adapters) | Domain isolated; adapters swappable |
| Distributed TX | Saga Choreography + Outbox Pattern | No dual-write problem |
| Mapper | MapStruct | Compile-time, zero reflection |
| Resilience | Resilience4j | Circuit breaker + retry + backoff |
| API Docs | SpringDoc OpenAPI 3 | Auto-generated Swagger UI |
| Testing | JUnit 5 + Mockito + EmbeddedKafka | Full coverage: unit → integration |
| Container | Docker + Docker Compose | Reproducible environment |

---

## Kafka Topics

| Topic | Publisher | Consumers |
|---|---|---|
| `subscription.created.v1` | Subscription Service | Payment Service |
| `subscription.activated.v1` | Subscription Service | Notification Service |
| `subscription.cancelled.v1` | Subscription Service | Notification Service |
| `subscription.failed.v1` | Subscription Service | Notification Service |
| `payment.completed.v1` | Payment Service | Subscription Service, Notification Service |
| `payment.failed.v1` | Payment Service | Subscription Service, Notification Service |
| `renewal.requested.v1` | Subscription Scheduler | Payment Service |

---

## Quick Start

```bash
# Full stack
docker-compose up --build

# Dev mode (H2 in-memory, single module)
./gradlew bootRun --args='--spring.profiles.active=dev'

# Run tests
./gradlew test
```

| URL | Description |
|---|---|
| `http://localhost:8080/swagger-ui.html` | Swagger UI — Subscription Service |
| `http://localhost:8081/swagger-ui.html` | Swagger UI — Payment Service |
| `http://localhost:8080/h2-console` | H2 DB Console (dev) |
| `http://localhost:9000` | Kafka UI |
| `http://localhost:8080/actuator/health` | Health check |

**Demo flow:**

```bash
# 1. Create subscription → 202 PENDING
curl -X POST http://localhost:8080/api/v1/subscriptions \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"userId":"user-uuid","planId":"plan-uuid","paymentMethod":{"cardToken":"tok_visa"}}'

# 2. Simulate payment webhook (Payment Service)
curl -X POST http://localhost:8081/api/v1/payments/webhook \
  -H "X-Signature: sha256=<hmac>" \
  -H "Content-Type: application/json" \
  -d '{"idempotencyKey":"key-1","status":"SUCCESS","subscriptionId":"sub-uuid","amount":99.99}'

# 3. Check subscription → 200 ACTIVE
curl http://localhost:8080/api/v1/subscriptions/sub-uuid \
  -H "Authorization: Bearer <token>"
```

---

## Documentation

| Document | Location |
|---|---|
| Product Requirements (PRD) | [`claude/prd.md`](claude/prd.md) |
| Architecture Decision Records | [`docs/architecture/ADR.md`](docs/architecture/ADR.md) |
| Subscription Service Design | [`docs/subscription-service/README.md`](docs/subscription-service/README.md) |
| Payment Service Design | [`docs/payment-service/README.md`](docs/payment-service/README.md) |
| Notification Service Design | [`docs/notification-service/README.md`](docs/notification-service/README.md) |

---

## Key Design Decisions

| Decision | Why |
|---|---|
| **Outbox Pattern** | Eliminates dual-write: DB state + Kafka event committed atomically |
| **Saga Choreography** | 3 services — no orchestrator needed; each service reacts to events |
| **`@Version` on Subscription** | Optimistic locking prevents concurrent cancel + renewal race condition |
| **No public `setStatus()`** | State machine enforced in domain entity; invalid transitions throw domain exception |
| **Mock notifications** | Hexagonal `NotificationPort` — swap to SendGrid/Twilio without touching use cases |
| **H2 → PostgreSQL** | Same Flyway migrations run in both; `application.properties` datasource change only |

---

> Author: Ahmet Özyılmaz · mini-sardis / subscription-service
