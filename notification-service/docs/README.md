# Notification Service — Design Document

> **Bounded Context:** User Notification Dispatch  
> **Port:** `:8082`  
> **Responsibilities:** Consume domain events from Kafka, dispatch mock email/SMS notifications (console log), maintain notification history.

---

## Table of Contents

1. [Purpose](#1-purpose)
2. [Domain Responsibilities](#2-domain-responsibilities)
3. [Hexagonal Layer Map](#3-hexagonal-layer-map)
4. [Notification Channels](#4-notification-channels)
5. [Key Use Cases](#5-key-use-cases)
6. [Kafka Events Consumed](#6-kafka-events-consumed)
7. [Outbound Ports](#7-outbound-ports)
8. [Notification Templates](#8-notification-templates)
9. [Database Tables](#9-database-tables)
10. [Key Design Decisions](#10-key-design-decisions)
11. [Running Locally](#11-running-locally)

---

## 1. Purpose

Notification Service is **purely reactive** — it listens to domain events and dispatches user notifications. It contains no business logic and owns no subscription or payment state.

**This service does:**
- Consume `subscription.*` and `payment.*` events from Kafka
- Dispatch mock email notifications (logged via `@Slf4j`)
- Dispatch mock SMS notifications (logged via `@Slf4j`)
- Record every notification attempt in `notification_logs` table

**This service does NOT do:**
- Manage subscription state
- Process payments
- Send real emails or SMS (explicitly out of scope — see ADR-010)
- Initiate actions — it only reacts to events

---

## 2. Domain Responsibilities

```
Notification Service
│
├── Event Consumer (Kafka — multiple topics)
├── Notification Dispatcher
│   ├── Email (MOCK — console log)
│   └── SMS (MOCK — console log)
└── Notification History (INSERT notification_logs)
```

**Failure isolation:** If Notification Service is down, subscription and payment operations continue unaffected. Kafka consumer lag will catch up when the service restarts — events are not lost.

---

## 3. Hexagonal Layer Map

| Layer | Package | Contents |
|-------|---------|----------|
| **Domain** | `domain/entity/` | `NotificationLog` (pure Java) |
| **Domain** | `domain/value/` | `NotificationChannel` (EMAIL, SMS), `NotificationStatus` |
| **Application** | `application/port/in/` | `SendSubscriptionNotificationUseCase`, `SendPaymentNotificationUseCase` |
| **Application** | `application/port/out/` | `NotificationRepositoryPort`, `EmailAdapterPort`, `SmsAdapterPort` |
| **Application** | `application/service/` | Use case implementations |
| **Infrastructure** | `infrastructure/adapter/in/kafka/` | `SubscriptionEventListener`, `PaymentEventListener` |
| **Infrastructure** | `infrastructure/adapter/out/jpa/` | `JpaNotificationLogRepository`, `NotificationLogJpaEntity` |
| **Infrastructure** | `infrastructure/adapter/out/email/` | `MockEmailAdapter` (implements `EmailAdapterPort`) |
| **Infrastructure** | `infrastructure/adapter/out/sms/` | `MockSmsAdapter` (implements `SmsAdapterPort`) |
| **Infrastructure** | `infrastructure/adapter/out/mapper/` | `NotificationLogJpaMapper` (MapStruct) |
| **Infrastructure** | `infrastructure/config/` | `KafkaConfig` |

---

## 4. Notification Channels

| Channel | Implementation | Future Adapter |
|---------|---------------|---------------|
| **EMAIL** | `MockEmailAdapter` — `log.info("MOCK EMAIL to {}: {}")` | `SendGridEmailAdapter implements EmailAdapterPort` |
| **SMS** | `MockSmsAdapter` — `log.info("MOCK SMS to {}: {}")` | `TwilioSmsAdapter implements SmsAdapterPort` |

**Hexagonal benefit:** Replacing mock with real implementations requires only:
1. Implement `EmailAdapterPort` in a new `SendGridEmailAdapter` class
2. Annotate with `@Primary` or use Spring profile `@Profile("production")`
3. No use case code changes

---

## 5. Key Use Cases

| Use Case | Trigger Event | Notification Sent |
|----------|--------------|-------------------|
| `SendActivationNotificationUseCase` | `subscription.activated.v1` | EMAIL — "Aboneliğiniz aktive edildi" |
| `SendCancellationNotificationUseCase` | `subscription.cancelled.v1` | EMAIL — "Aboneliğiniz iptal edildi" |
| `SendPaymentFailedNotificationUseCase` | `subscription.failed.v1` | EMAIL — "Ödeme başarısız, abonelik oluşturulamadı" |
| `SendRenewalSuccessNotificationUseCase` | `subscription.renewed.v1` | EMAIL — "Aboneliğiniz yenilendi" |
| `SendSuspensionNotificationUseCase` | `subscription.suspended.v1` | EMAIL + SMS — "Ödeme alınamadı, abonelik askıya alındı" |

---

## 6. Kafka Events Consumed

Consumer group: `notification-sender`

| Topic | Handler | Notification |
|-------|---------|-------------|
| `subscription.activated.v1` | `SubscriptionEventListener` | EMAIL — subscription activated |
| `subscription.cancelled.v1` | `SubscriptionEventListener` | EMAIL — subscription cancelled |
| `subscription.failed.v1` | `SubscriptionEventListener` | EMAIL — payment failed, subscription not created |
| `subscription.renewed.v1` | `SubscriptionEventListener` | EMAIL — subscription renewed |
| `subscription.suspended.v1` | `SubscriptionEventListener` | EMAIL + SMS — suspension warning |

**Idempotency:** Notification dispatch is idempotent by design — duplicate event delivery results in a duplicate `notification_logs` record (acceptable for logging) but no side effect for mock dispatch.

---

## 7. Outbound Ports

| Port | Interface | Adapter |
|------|-----------|---------|
| `NotificationRepositoryPort` | `save(NotificationLog)`, `findByUserId(UUID, Pageable)` | `JpaNotificationLogRepository` |
| `EmailAdapterPort` | `send(String to, String subject, String body)` | `MockEmailAdapter` |
| `SmsAdapterPort` | `send(String phoneNumber, String message)` | `MockSmsAdapter` |

**REST API (notification history):**

| Method | Path | Response | Auth |
|--------|------|----------|------|
| `GET` | `/api/v1/notifications?userId={id}` | `Page<NotificationLogResponse>` | USER |

---

## 8. Notification Templates

All templates in Turkish (internationalization out of scope):

| Event | Channel | Template |
|-------|---------|---------|
| `subscription.activated.v1` | EMAIL | `Sayın {fullName}, "{planName}" aboneliğiniz başarıyla aktive edildi. Sonraki yenileme tarihi: {nextRenewalDate}.` |
| `subscription.cancelled.v1` | EMAIL | `Sayın {fullName}, aboneliğiniz iptal edildi. Tekrar abone olmak için uygulamamızı ziyaret edebilirsiniz.` |
| `subscription.failed.v1` | EMAIL | `Sayın {fullName}, ödeme işlemi başarısız oldu. Aboneliğiniz oluşturulamadı. Lütfen ödeme bilgilerinizi kontrol edin.` |
| `subscription.renewed.v1` | EMAIL | `Sayın {fullName}, aboneliğiniz {nextRenewalDate} tarihine kadar uzatıldı.` |
| `subscription.suspended.v1` | EMAIL | `Sayın {fullName}, ödeme alınamadı, aboneliğiniz askıya alındı.` |
| `subscription.suspended.v1` | SMS | `Aboneliğiniz askıya alındı. Ödeme bilgilerinizi güncelleyin.` |

---

## 9. Database Tables

| Table | Purpose |
|-------|---------|
| `notification_logs` | Audit trail of all notification attempts (channel, success, timestamp) |

**Key index:** `idx_notification_logs_user_id ON notification_logs(user_id)` — used for history queries.

---

## 10. Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| Event-driven (not direct call) | Decouples Notification Service from Subscription/Payment lifecycle; failure isolated |
| Mock via `@Slf4j` | ADR-010: real email out of scope; hexagonal port allows future swap to SendGrid/Twilio |
| `notification_logs` table | Provides audit trail; test assertions can verify notifications were "sent" |
| Single consumer group for all subscription events | All notification logic co-located; easy to add new event handlers |
| No outbound Kafka publishing | Notification is a terminal service in the event chain — no events published |

---

## 11. Running Locally

**Console output shows mock notifications:**
```
INFO  MockEmailAdapter - MOCK EMAIL to user1@demo.com
      Subject: Aboneliğiniz aktive edildi
      Body: Sayın Demo User, "Pro" aboneliğiniz başarıyla aktive edildi...

INFO  MockSmsAdapter  - MOCK SMS to +90555-xxx-xxxx
      Message: Aboneliğiniz askıya alındı. Ödeme bilgilerinizi güncelleyin.
```

**View notification history:**
```bash
curl "http://localhost:8082/api/v1/notifications?userId=user-uuid" \
  -H "Authorization: Bearer <token>"
```

**Response:**
```json
{
  "content": [
    {
      "id": "notif-uuid",
      "channel": "EMAIL",
      "subject": "Aboneliğiniz aktive edildi",
      "success": true,
      "sentAt": "2026-04-24T09:15:30Z"
    }
  ],
  "totalElements": 3
}
```

**Health check:**
```bash
curl http://localhost:8082/actuator/health
```

**Verifying notifications in H2:**
```sql
-- In H2 console at http://localhost:8082/h2-console
SELECT * FROM notification_logs WHERE user_id = 'user-uuid' ORDER BY sent_at DESC;
```
