# Architecture Decision Records (ADR)
# Mimari Karar Kayıtları

> **Proje / Project:** Abonelik Yönetim Sistemi / Subscription Management System  
> **Format:** Title | Status | Context | Decision | Rationale | Consequences

---

## ADR-001 — Microservices over Monolith

**Status:** Accepted  
**Date:** 2026-04-24

### Context
The system has three distinct bounded contexts: subscription lifecycle management, payment processing, and notification dispatch. Each has different:
- **Scale profiles** — Payment processing peaks at billing cycles; notification is bursty.
- **Failure modes** — Payment provider downtime must not block subscription reads.
- **Change velocity** — Notification templates change more frequently than payment logic.

### Decision
Implement three independently deployable microservices: Subscription Service, Payment Service, Notification Service.

### Rationale
| Driver | Microservice Benefit |
|--------|---------------------|
| Independent scaling | Payment Service scales horizontally during batch renewals |
| Fault isolation | Notification failure does not degrade subscription queries |
| Independent deployment | Bug fix in Payment Service requires no redeploy of others |
| Team boundaries | Each service can be owned by a separate team |

### Consequences
- (+) Independent scaling, deployment, and fault isolation
- (+) Each service can choose its optimal runtime config and DB schema
- (-) Distributed transaction complexity → addressed by Kafka Saga + Outbox Pattern (see ADR-004, ADR-005)
- (-) More operational overhead → addressed by Docker Compose and Spring Actuator health endpoints
- **Monolith-first note:** The project starts as a single Spring Boot module to keep development friction low. Internal structure uses hexagonal packages (see ADR-002), enabling extraction into separate modules later with minimal refactoring.

---

## ADR-002 — Hexagonal Architecture (Ports & Adapters) per Service

**Status:** Accepted  
**Date:** 2026-04-24

### Context
Without architectural discipline, Spring Boot applications grow into "big ball of mud" where JPA entities leak into controllers, business logic depends on Kafka consumer annotations, and tests require a full Spring context to run.

### Decision
Apply Hexagonal Architecture (Ports & Adapters) inside each microservice. Three layers:
- **Domain** — pure Java; entities, value objects, domain events, domain services; zero Spring dependencies.
- **Application** — use cases (one class per operation), inbound/outbound port interfaces.
- **Infrastructure** — Spring-specific adapters: REST controllers, Kafka listeners, JPA repositories, external clients.

### Rationale
| Problem | Hexagonal Solution |
|---------|-------------------|
| Business logic depends on JPA | Domain entities have no `@Entity` — JPA entities in infrastructure |
| Tests need Spring context | Domain and use case classes use plain JUnit with Mockito |
| Swapping Kafka for RabbitMQ | Change only `KafkaEventPublisher` adapter; use case untouched |
| Technology upgrades | Spring Boot version upgrade isolated to infrastructure layer |

### Consequences
- (+) Domain classes are 100% unit-testable without Spring context
- (+) Outbound adapters (JPA, Kafka, HTTP client) are swappable behind port interfaces
- (+) Each adapter has a clear, single responsibility
- (-) More files and interfaces than a traditional layered architecture
- (-) Developers must understand ports-and-adapters mental model
- **MapStruct mappers** bridge domain ↔ JPA entity ↔ REST DTO conversions (see ADR-006)

---

## ADR-003 — Apache Kafka over RabbitMQ / In-Process Spring Events

**Status:** Accepted  
**Date:** 2026-04-24

### Context
Services need to communicate asynchronously. Three options considered:
1. **In-process Spring Events** (`ApplicationEventPublisher`)
2. **RabbitMQ** — AMQP message broker
3. **Apache Kafka** — distributed event log

### Decision
Use Apache Kafka as the message broker.

### Rationale

| Criterion | Spring Events | RabbitMQ | Kafka (chosen) |
|-----------|--------------|----------|---------------|
| Fan-out (1 event → N consumers) | ❌ In-memory only | ⚠️ Requires fanout exchange config | ✅ Native pub-sub |
| Event replay | ❌ Lost on restart | ❌ Messages consumed and deleted | ✅ Configurable retention |
| At-least-once guarantee | ❌ | ✅ | ✅ |
| Durability (restart survives) | ❌ | ✅ | ✅ |
| Ecosystem (Spring Kafka) | N/A | ✅ | ✅ |

**Key driver:** Payment events need to fan out to both Subscription Service AND Notification Service. Kafka native publish-subscribe handles this cleanly without fanout exchange configuration.

### Consequences
- (+) Decoupled services — adding a new consumer (e.g., Audit Service) requires no change to publisher
- (+) Event replay capability for debugging and re-processing
- (-) Kafka + Zookeeper infrastructure required → added to Docker Compose
- (-) EmbeddedKafka required for integration tests
- **Topic versioning:** All topics suffixed with `.v1` to support backward-compatible evolution

---

## ADR-004 — Saga Choreography over Orchestration

**Status:** Accepted  
**Date:** 2026-04-24

### Context
Subscription creation is a multi-step distributed transaction:
1. Create subscription record (PENDING)
2. Initiate payment
3. Activate subscription on payment success / cancel on failure
4. Send notification

Two saga approaches:
- **Orchestration** — A central Saga Orchestrator service directs each step.
- **Choreography** — Each service reacts to events published by the previous step.

### Decision
Use Saga Choreography for the subscription creation and renewal flows.

### Rationale
| Factor | Orchestration | Choreography (chosen) |
|--------|--------------|----------------------|
| Complexity for 3 services | Over-engineered | Appropriate |
| Single point of failure | Orchestrator | Distributed (more resilient) |
| Visibility | Centralized trace | Kafka topic monitoring |
| Team autonomy | Orchestrator owns flow | Each service owns its step |

With only 3 services participating in the saga, choreography avoids introducing an orchestrator component that would become a central coupling point and deployment bottleneck.

### Consequences
- (+) Each service is independently deployable; no central coordinator
- (+) Adding a step (e.g., fraud check) means a new Kafka consumer, not an orchestrator change
- (-) Distributed saga logic is harder to visualize without observability tooling
- **Compensating transactions:** Subscription cancel on payment failure serves as the compensating transaction (PENDING → CANCELLED)

---

## ADR-005 — Outbox Pattern for Reliable Event Publication

**Status:** Accepted  
**Date:** 2026-04-24

### Context
The "dual-write problem": if a service writes to the database AND publishes to Kafka in two separate operations, one can succeed while the other fails:

```
INSERT subscription (PENDING)  ← DB write succeeds
publish subscription.created   ← Kafka publish FAILS → Payment Service never notified
                                  → Subscription stuck in PENDING forever
```

### Decision
Implement the Transactional Outbox Pattern:
1. Write domain state AND outbox event record in **the same DB transaction**.
2. A separate `OutboxPoller` (@Scheduled, every 5s) reads unprocessed outbox records and publishes to Kafka.
3. Mark records as processed after successful publish.

### Rationale
- Outbox + DB transaction = atomic "write state + record intent"
- OutboxPoller provides at-least-once delivery guarantee
- If Kafka is temporarily unavailable, outbox records accumulate and are replayed when Kafka recovers

### Consequences
- (+) Eliminates dual-write problem — event publication is guaranteed
- (+) Kafka downtime does not lose events — they wait in the outbox table
- (-) At-least-once delivery → consumers must be idempotent (handled via `idempotencyKey` on Payment)
- (-) Outbox table grows if poller is down — requires monitoring and periodic cleanup
- **`idx_outbox_unprocessed` index** ensures the poller query is fast even with millions of processed records

---

## ADR-006 — MapStruct over ModelMapper / Manual Mappers

**Status:** Accepted  
**Date:** 2026-04-24

### Context
Hexagonal architecture introduces multiple representations of the same concept:
- Domain entity (`Subscription`) — framework-agnostic
- JPA entity (`SubscriptionJpaEntity`) — with `@Entity`, `@Table` annotations
- REST DTO (`SubscriptionResponse`) — for API consumers
- Kafka event DTO (`SubscriptionCreatedEventDto`) — for event payloads

Mapping between these is necessary. Options:
1. **ModelMapper** — runtime reflection-based
2. **Manual mappers** — verbose but explicit
3. **MapStruct** — compile-time code generation

### Decision
Use MapStruct for all object mapping.

### Rationale
| Criterion | ModelMapper | Manual | MapStruct (chosen) |
|-----------|------------|--------|-------------------|
| Performance | Runtime reflection (slow) | Explicit (fast) | Compile-time generated (fast) |
| Type safety | Runtime errors | Compile-time | Compile-time |
| Missing field detection | Runtime | Manual review | Compile error |
| Spring DI integration | ✅ | Manual | ✅ `componentModel="spring"` |
| Boilerplate | Low | High | Low |

### Consequences
- (+) Compile-time errors for missing field mappings — caught before runtime
- (+) Zero runtime reflection overhead
- (+) Spring-managed mapper beans work naturally in hexagonal adapters
- (-) Requires `@Mapping` annotations for non-trivial field name differences
- (-) Adds annotation processor dependency (`mapstruct-processor`)

---

## ADR-007 — H2 for Development / PostgreSQL-Compatible Schema for Production

**Status:** Accepted  
**Date:** 2026-04-24

### Context
The project needs a database for both development speed and production readiness.

### Decision
- **Development:** H2 in-memory database (auto-configured by Spring Boot)
- **Production-ready:** Write all Flyway migrations as PostgreSQL-compatible SQL (avoid H2-only syntax)
- Partial indexes (PostgreSQL `WHERE` clause) documented as production-only; H2 equivalent provided

### Rationale
- H2: zero setup, instant startup, perfect for CI and local development
- PostgreSQL-compatible SQL: switching from H2 to PostgreSQL requires only `application.properties` datasource change
- Flyway ensures schema version consistency across all environments

### Consequences
- (+) Instant developer onboarding — no local DB setup
- (+) Identical Flyway migrations run in both dev (H2) and prod (PostgreSQL)
- (-) H2 has subtle SQL dialect differences — integration tests cover critical query patterns
- **Note:** H2 doesn't support `CREATE INDEX ... WHERE ...` (partial indexes). Use compound indexes in H2; PostgreSQL-specific partial indexes added in a separate migration profile.

---

## ADR-008 — Spring Cache (ConcurrentMap) over Redis for Development

**Status:** Accepted  
**Date:** 2026-04-24

### Context
`SubscriptionPlan` data is read on every subscription creation but changes rarely. Caching is appropriate. Options: in-memory (`ConcurrentMapCache`), Redis, Caffeine.

### Decision
Use Spring Cache abstraction with `ConcurrentMapCacheManager` in development. Production can swap to Redis without changing use case code.

### Rationale
- Spring Cache abstraction (`@Cacheable`, `@CacheEvict`) is backend-agnostic
- In dev/H2 profile: `ConcurrentMapCache` requires zero infrastructure
- In production: `RedisCacheManager` bean swaps in with only a `CacheConfig` change
- No cache at all for subscription status (changes via async events — stale risk)

### Consequences
- (+) Hexagonal benefit: caching is infrastructure; use cases use only `@Cacheable` annotation
- (+) Tests can disable cache with `NullCacheManager` without changing use case code
- (-) ConcurrentMap cache is not distributed — not suitable for multi-instance production deployment → Redis for prod
- **What NOT to cache:** subscription status (async event updates), payment records (idempotency key must hit DB)

---

## ADR-009 — JWT Bearer Authentication over Session-Based Auth

**Status:** Accepted  
**Date:** 2026-04-24

### Context
API security is required. Options: session-based (server-side state), JWT (stateless), OAuth2.

### Decision
JWT Bearer token authentication via Spring Security.

### Rationale
- Stateless — no server-side session storage required
- Scales horizontally without sticky sessions or shared session store
- CSRF attacks irrelevant for stateless JWT (no cookie-based session)
- Simpler than full OAuth2 for this case scope

### Consequences
- (+) Horizontal scaling without session affinity
- (+) CSRF protection not needed (documented as conscious decision)
- (-) Token revocation requires a token blacklist (out of scope)
- (-) JWT secret must be securely managed (environment variable, not hardcoded)
- **Roles:** `ROLE_USER` for user endpoints, `ROLE_ADMIN` for management endpoints

---

## ADR-010 — Mock Notifications (Console Log) — No External Email/SMS Integration

**Status:** Accepted  
**Date:** 2026-04-24

### Context
The case requires user notification on subscription events. Real email/SMS integration (SendGrid, Twilio) introduces external dependencies, credentials management, and test complexity.

### Decision
Notification Service uses `@Slf4j` console log output as mock email/SMS. All notification attempts are recorded in `notification_logs` table for history queries.

### Rationale
- Case evaluation focuses on system design, not email deliverability
- Hexagonal `NotificationPort` interface allows future adapter swap without use case changes
- `notification_logs` table proves notification behavior without actual sends
- Zero external credentials or rate limit concerns during demo

### Consequences
- (+) Zero external service dependencies — demo runs fully offline
- (+) Swap to SendGrid/Twilio by implementing a new `NotificationPort` adapter
- (+) `notification_logs` provides audit trail for test assertions
- (-) Not a production email solution — explicitly noted as out of scope
- **Future path:** `EmailAdapter implements NotificationPort` → inject via `@Primary` or Spring profile
