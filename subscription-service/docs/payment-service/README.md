# Payment Service — Design Document

> **Bounded Context:** Payment Processing  
> **Port:** `:8081`  
> **Responsibilities:** Initiate payment charges against mock external provider, handle payment webhook callbacks, enforce idempotency, publish payment result events.

---

## Table of Contents

1. [Purpose](#1-purpose)
2. [Domain Responsibilities](#2-domain-responsibilities)
3. [Hexagonal Layer Map](#3-hexagonal-layer-map)
4. [Idempotency Strategy](#4-idempotency-strategy)
5. [Key Use Cases](#5-key-use-cases)
6. [Inbound Ports (API + Kafka)](#6-inbound-ports-api--kafka)
7. [Outbound Ports](#7-outbound-ports)
8. [Kafka Events Published](#8-kafka-events-published)
9. [Kafka Events Consumed](#9-kafka-events-consumed)
10. [Resilience — Retry & Circuit Breaker](#10-resilience--retry--circuit-breaker)
11. [Database Tables](#11-database-tables)
12. [Security — Webhook Verification](#12-security--webhook-verification)
13. [Key Design Decisions](#13-key-design-decisions)
14. [Running Locally](#14-running-locally)

---

## 1. Purpose

Payment Service is the **payment processing gateway** of the system. It translates external payment provider results into domain events that the rest of the system can react to.

**This service does:**
- Consume `subscription.created.v1` and `renewal.requested.v1` events → initiate charge
- Receive payment webhook from external provider → verify, deduplicate, process
- Publish `payment.completed.v1` or `payment.failed.v1` events
- Enforce idempotency on all payment operations
- Apply retry with exponential backoff and circuit breaker on external provider calls

**This service does NOT do:**
- Manage subscription state (Subscription Service responsibility)
- Send notifications (Notification Service responsibility)
- Store card data (card tokens only; no PAN stored)

---

## 2. Domain Responsibilities

```
Payment Service
│
├── Initiate Payment (INITIAL type — on new subscription)
├── Initiate Renewal (RENEWAL type — on renewal request)
├── Handle Webhook (validate HMAC, check idempotency, update status)
├── Publish Payment Result Events (payment.completed.v1 / payment.failed.v1)
└── Retry Logic (Resilience4j @Retry, max 3, exponential backoff)
```

**Payment lifecycle:**
```
INSERT payment (PENDING)
  → charge() [with retry]
    → SUCCESS → UPDATE payment(SUCCESS) → publish payment.completed.v1
    → FAILED (max retries) → UPDATE payment(FAILED) → publish payment.failed.v1
```

---

## 3. Hexagonal Layer Map

| Layer | Package | Contents |
|-------|---------|----------|
| **Domain** | `domain/entity/` | `Payment` (pure Java) |
| **Domain** | `domain/value/` | `PaymentStatus`, `PaymentType`, `Money` |
| **Domain** | `domain/event/` | `PaymentCompletedEvent`, `PaymentFailedEvent` |
| **Domain** | `domain/service/` | `PaymentDomainService` — idempotency check, status rules |
| **Application** | `application/port/in/` | `ProcessWebhookUseCase`, `InitiatePaymentUseCase` |
| **Application** | `application/port/out/` | `PaymentRepositoryPort`, `ExternalPaymentProviderPort`, `EventPublisherPort` |
| **Application** | `application/service/` | Use case implementations |
| **Infrastructure** | `infrastructure/adapter/in/web/` | `PaymentWebhookController` |
| **Infrastructure** | `infrastructure/adapter/in/kafka/` | `SubscriptionCreatedListener`, `RenewalRequestedListener` |
| **Infrastructure** | `infrastructure/adapter/in/mapper/` | `WebhookRequestMapper` (MapStruct) |
| **Infrastructure** | `infrastructure/adapter/out/jpa/` | `JpaPaymentRepository`, `PaymentJpaEntity` |
| **Infrastructure** | `infrastructure/adapter/out/kafka/` | `KafkaPaymentEventPublisher` |
| **Infrastructure** | `infrastructure/adapter/out/payment/` | `MockExternalPaymentClient` (implements `ExternalPaymentProviderPort`) |
| **Infrastructure** | `infrastructure/adapter/out/mapper/` | `PaymentJpaMapper` (MapStruct) |
| **Infrastructure** | `infrastructure/config/` | `ResilienceConfig`, `KafkaConfig`, `SecurityConfig` |

---

## 4. Idempotency Strategy

Payment webhook handlers are idempotent — calling the same webhook twice has no side effect after the first processing.

```
POST /api/v1/payments/webhook {idempotencyKey: "pay_xyz", status: "SUCCESS"}

Step 1: Verify HMAC-SHA256 signature header
        → Invalid signature → 401 Unauthorized

Step 2: SELECT payment WHERE idempotency_key = 'pay_xyz'
        → Record NOT found → Create new payment record → Process

Step 3: SELECT payment WHERE idempotency_key = 'pay_xyz'
  (second call, same key)
        → Record FOUND and status != PENDING → Return 200 (no-op)
        → Record FOUND and status = PENDING → Process (in-flight retry)

Step 4: Process payment
        → UPDATE payment status (SUCCESS/FAILED)
        → Publish payment.completed.v1 or payment.failed.v1
```

**Why DB-backed idempotency (not cache):**  
Cache (Redis/in-memory) can be evicted between the check and the processing, leading to duplicate charges. A DB record with a UNIQUE constraint on `idempotency_key` is the authoritative, durable record.

---

## 5. Key Use Cases

| Use Case | Trigger | Action |
|----------|---------|--------|
| `InitiatePaymentUseCase` | `subscription.created.v1` event | INSERT payment (PENDING, INITIAL), call provider |
| `InitiateRenewalPaymentUseCase` | `renewal.requested.v1` event | INSERT payment (PENDING, RENEWAL), call provider with retry |
| `ProcessWebhookUseCase` | `POST /api/v1/payments/webhook` | Verify HMAC, dedup, UPDATE status, publish result event |
| `GetPaymentUseCase` | `GET /api/v1/payments/{id}` | Read payment record |

---

## 6. Inbound Ports (API + Kafka)

**REST:**

| Method | Path | Request | Response | Auth |
|--------|------|---------|----------|------|
| `POST` | `/api/v1/payments/webhook` | `WebhookRequest` + `X-Signature` header | `200` | HMAC |
| `GET` | `/api/v1/payments/{id}` | — | `200 PaymentResponse` | ADMIN |

**Kafka (consumed):**

| Topic | Consumer Group | Use Case Triggered |
|-------|---------------|-------------------|
| `subscription.created.v1` | `payment-subscription-processor` | `InitiatePaymentUseCase` |
| `renewal.requested.v1` | `payment-renewal-processor` | `InitiateRenewalPaymentUseCase` |

---

## 7. Outbound Ports

| Port | Interface | Adapter |
|------|-----------|---------|
| `PaymentRepositoryPort` | `save(Payment)`, `findByIdempotencyKey(String)`, `findById(UUID)` | `JpaPaymentRepository` |
| `ExternalPaymentProviderPort` | `charge(cardToken, amount, currency)` | `MockExternalPaymentClient` |
| `EventPublisherPort` | `publish(PaymentEvent)` | `KafkaPaymentEventPublisher` |

**`MockExternalPaymentClient`:**  
Simulates the external payment provider. In dev profile, returns configurable success/failure responses. In production (future), replace with `StripePaymentClient implements ExternalPaymentProviderPort` without touching use cases.

---

## 8. Kafka Events Published

| Topic | Trigger | Payload |
|-------|---------|---------|
| `payment.completed.v1` | Webhook with SUCCESS status | `{subscriptionId, paymentId, amount, currency, type: INITIAL/RENEWAL, processedAt}` |
| `payment.failed.v1` | Webhook with FAILED status OR max retries exceeded | `{subscriptionId, paymentId, reason, retryCount, type: INITIAL/RENEWAL}` |

---

## 9. Kafka Events Consumed

| Topic | Action |
|-------|--------|
| `subscription.created.v1` | Extract `subscriptionId`, `planId`, `cardToken`, `amount` → initiate INITIAL payment |
| `renewal.requested.v1` | Extract `subscriptionId`, `amount` → initiate RENEWAL payment with retry |

---

## 10. Resilience — Retry & Circuit Breaker

Applied to `ExternalPaymentProviderPort.charge()` via Resilience4j:

```java
@Retry(name = "paymentProvider", fallbackMethod = "chargeFallback")
@CircuitBreaker(name = "paymentProvider", fallbackMethod = "chargeFallback")
public PaymentResult charge(String cardToken, BigDecimal amount, String currency) {
    return externalClient.charge(cardToken, amount, currency);
}
```

**Retry configuration:**
```yaml
resilience4j.retry:
  instances:
    paymentProvider:
      max-attempts: 3
      wait-duration: 1s
      enable-exponential-backoff: true
      exponential-backoff-multiplier: 2.0
      retry-exceptions:
        - java.io.IOException
        - java.util.concurrent.TimeoutException
```

**Circuit Breaker configuration:**
```yaml
resilience4j.circuitbreaker:
  instances:
    paymentProvider:
      failure-rate-threshold: 50
      slow-call-duration-threshold: 3s
      slow-call-rate-threshold: 80
      sliding-window-size: 10
      wait-duration-in-open-state: 30s
```

**Fallback behavior:** On circuit open or max retries exceeded → publish `payment.failed.v1` immediately.

---

## 11. Database Tables

| Table | Purpose |
|-------|---------|
| `payments` | Payment records with idempotency key, status, type, retry count |
| `outbox_events` | (if Payment Service uses Outbox) Optional — for guaranteed event publish |

**Critical constraint:** `UNIQUE(idempotency_key)` on `payments` — database-level guard against duplicate processing.

---

## 12. Security — Webhook Verification

Every incoming webhook from the external payment provider includes an `X-Signature` header:

```
X-Signature: sha256=<HMAC-SHA256(requestBody, webhookSecret)>
```

Verification (Spring Security filter or `@Component WebhookSignatureVerifier`):

```java
String expected = "sha256=" + hmacSha256(requestBody, webhookSecret);
if (!MessageDigest.isEqual(expected.getBytes(), received.getBytes())) {
    throw new WebhookSignatureException("Invalid webhook signature");
}
```

`webhookSecret` is loaded from environment variable — never hardcoded.

---

## 13. Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| DB-backed idempotency (not cache) | Cache eviction between check and process = duplicate charge risk |
| HMAC-SHA256 webhook verification | Prevents replay attacks from malicious actors |
| Circuit breaker on external provider | External provider downtime must not cascade to Kafka consumer lag |
| `MockExternalPaymentClient` | Swap to real provider by implementing `ExternalPaymentProviderPort` — use cases unchanged |
| Separate consumer groups for subscription vs renewal | Independent lag monitoring and scaling per payment type |
| No PAN storage | Only tokenized card references (provider-issued tokens); no PCI scope |

---

## 14. Running Locally

```bash
# Start Payment Service (within Docker Compose)
docker-compose up payment-service

# Webhook simulation
curl -X POST http://localhost:8081/api/v1/payments/webhook \
  -H "Content-Type: application/json" \
  -H "X-Signature: sha256=<computed_hmac>" \
  -d '{
    "idempotencyKey": "pay_demo_001",
    "externalRef": "pi_mock_001",
    "status": "SUCCESS",
    "subscriptionId": "sub-uuid",
    "amount": 99.99,
    "currency": "TRY"
  }'

# Health check
curl http://localhost:8081/actuator/health

# Swagger UI
open http://localhost:8081/swagger-ui.html
```

**Testing idempotency:**
```bash
# Call webhook twice with same idempotencyKey
# First call: processes and returns 200
# Second call: returns 200 without reprocessing (check payment table has only 1 record)
curl -X POST http://localhost:8081/api/v1/payments/webhook -d '{"idempotencyKey":"dup_test_001",...}'
curl -X POST http://localhost:8081/api/v1/payments/webhook -d '{"idempotencyKey":"dup_test_001",...}'
# Both return 200; payment table has exactly 1 record
```
