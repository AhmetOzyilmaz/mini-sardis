# Deployment Guide / Dağıtım Kılavuzu

> **Proje / Project:** Abonelik Yönetim Sistemi / Subscription Management System  
> **Stack:** Spring Boot 4.x · Java 17 · Apache Kafka · H2 / PostgreSQL · Docker Compose

---

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Quick Start — Docker Compose](#2-quick-start--docker-compose)
3. [Service URLs](#3-service-urls)
4. [Environment Variables](#4-environment-variables)
5. [Local Development (without Docker)](#5-local-development-without-docker)
6. [Running Tests](#6-running-tests)
7. [Demo Walkthrough](#7-demo-walkthrough)
8. [Dockerfile (per service)](#8-dockerfile-per-service)
9. [Docker Compose Configuration](#9-docker-compose-configuration)
10. [Monitoring & Health Checks](#10-monitoring--health-checks)
11. [Troubleshooting](#11-troubleshooting)

---

## 1. Prerequisites

| Tool | Version | Installation |
|------|---------|-------------|
| Docker Desktop | Latest | https://www.docker.com/products/docker-desktop |
| Java | 17 (LTS) | https://adoptium.net |
| Gradle | via wrapper (`./gradlew`) | Included in repo |
| curl / Postman | Any | For API testing |

---

## 2. Quick Start — Docker Compose

```bash
# Clone the repository
git clone <repo-url>
cd mini-sardis/subscription-service

# Build and start all services
docker-compose up --build

# Run in background
docker-compose up --build -d

# Stop all services
docker-compose down

# Stop and remove volumes (fresh start)
docker-compose down -v
```

**First startup takes ~2 minutes** while Docker pulls base images and Kafka initializes.

---

## 3. Service URLs

| Service | URL | Description |
|---------|-----|-------------|
| Subscription Service | http://localhost:8080 | Main subscription API |
| Payment Service | http://localhost:8081 | Payment webhook + API |
| Notification Service | http://localhost:8082 | Notification history |
| Swagger UI | http://localhost:8080/swagger-ui.html | Interactive API docs |
| H2 Console | http://localhost:8080/h2-console | Database browser (dev) |
| Kafka UI | http://localhost:9000 | Topic & message browser |
| Health — Subscription | http://localhost:8080/actuator/health | |
| Health — Payment | http://localhost:8081/actuator/health | |
| Health — Notification | http://localhost:8082/actuator/health | |

**H2 Console connection:**
```
JDBC URL:  jdbc:h2:mem:subscriptiondb
Username:  sa
Password:  (leave empty)
```

---

## 4. Environment Variables

Configure via `.env` file in project root (copy from `.env.example`):

**Subscription Service:**
```env
SPRING_PROFILES_ACTIVE=dev
JWT_SECRET=your-256-bit-secret-here
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
SPRING_DATASOURCE_URL=jdbc:h2:mem:subscriptiondb
OUTBOX_POLL_INTERVAL_MS=5000
RENEWAL_CRON=0 0 9 * * *
```

**Payment Service:**
```env
SPRING_PROFILES_ACTIVE=dev
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
SPRING_DATASOURCE_URL=jdbc:h2:mem:paymentdb
WEBHOOK_SECRET=your-webhook-hmac-secret
PAYMENT_PROVIDER_BASE_URL=http://mock-provider:8090
RESILIENCE4J_RETRY_MAX_ATTEMPTS=3
```

**Notification Service:**
```env
SPRING_PROFILES_ACTIVE=dev
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
SPRING_DATASOURCE_URL=jdbc:h2:mem:notificationdb
```

> **Security:** Never commit real secrets to version control. Use environment variables or a secrets manager in production.

---

## 5. Local Development (without Docker)

```bash
# Run with dev profile (H2 in-memory, mock providers)
./gradlew bootRun --args='--spring.profiles.active=dev'

# Run with specific service (if multi-module)
./gradlew :subscription-service:bootRun --args='--spring.profiles.active=dev'

# Build without tests
./gradlew build -x test

# Build fat JAR
./gradlew bootJar
```

**Kafka for local dev:** Use Docker just for Kafka+Zookeeper:
```bash
docker-compose up kafka zookeeper
```

---

## 6. Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "com.mini.sardis.application.service.CreateSubscriptionUseCaseTest"

# Run real-world failure scenario tests
./gradlew test --tests "*RealWorld*"

# Run with test report
./gradlew test jacocoTestReport
open build/reports/tests/test/index.html

# Run integration tests only (component tests)
./gradlew test --tests "*IntegrationTest"
```

**Test profiles:**
- Unit tests: no Spring context, Mockito mocks, pure JUnit
- Component tests: `@SpringBootTest` + H2 + EmbeddedKafka
- Repository tests: `@DataJpaTest` + H2

---

## 7. Demo Walkthrough

Step-by-step demo using curl (requires Docker Compose running):

### Step 1 — Get JWT Token

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user1@demo.com","password":"demo123"}'
# → {"token":"eyJhbGci..."}
export TOKEN="eyJhbGci..."
```

### Step 2 — View Available Plans

```bash
curl http://localhost:8080/api/v1/plans
# → [
#     {"id":"plan-basic-uuid","name":"Basic","price":49.99},
#     {"id":"plan-pro-uuid","name":"Pro","price":99.99},
#     {"id":"plan-enterprise-uuid","name":"Enterprise","price":249.99}
#   ]
```

### Step 3 — Create Subscription

```bash
curl -X POST http://localhost:8080/api/v1/subscriptions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-demo-uuid",
    "planId": "plan-pro-uuid",
    "paymentMethod": {"cardToken": "tok_visa_success"}
  }'
# → 202 {"subscriptionId":"sub-new-uuid","status":"PENDING","message":"Aboneliğiniz oluşturuluyor"}
export SUB_ID="sub-new-uuid"
```

### Step 4 — Check Status (PENDING)

```bash
curl http://localhost:8080/api/v1/subscriptions/$SUB_ID \
  -H "Authorization: Bearer $TOKEN"
# → {"status":"PENDING",...}
```

### Step 5 — Simulate Payment Webhook (SUCCESS)

```bash
BODY='{"idempotencyKey":"pay-demo-001","externalRef":"pi_mock_001","status":"SUCCESS","subscriptionId":"'$SUB_ID'","amount":99.99,"currency":"TRY"}'
SIGNATURE=$(echo -n "$BODY" | openssl dgst -sha256 -hmac "your-webhook-hmac-secret" | awk '{print $2}')

curl -X POST http://localhost:8081/api/v1/payments/webhook \
  -H "Content-Type: application/json" \
  -H "X-Signature: sha256=$SIGNATURE" \
  -d "$BODY"
# → 200 OK
```

### Step 6 — Check Status (ACTIVE)

```bash
# Wait 5-10 seconds for Outbox Poller and Kafka events to propagate
sleep 10
curl http://localhost:8080/api/v1/subscriptions/$SUB_ID \
  -H "Authorization: Bearer $TOKEN"
# → {"status":"ACTIVE","nextRenewalDate":"2026-05-24",...}
```

### Step 7 — Check Notification Log

```bash
curl "http://localhost:8082/api/v1/notifications?userId=user-demo-uuid" \
  -H "Authorization: Bearer $TOKEN"
# → {"content":[{"channel":"EMAIL","subject":"Aboneliğiniz aktive edildi","success":true}],...}
```

### Step 8 — Cancel Subscription

```bash
curl -X DELETE http://localhost:8080/api/v1/subscriptions/$SUB_ID \
  -H "Authorization: Bearer $TOKEN"
# → 200 OK

curl http://localhost:8080/api/v1/subscriptions/$SUB_ID \
  -H "Authorization: Bearer $TOKEN"
# → {"status":"CANCELLED",...}
```

### Step 9 — Inspect H2 Database

Open http://localhost:8080/h2-console and run:
```sql
SELECT * FROM subscriptions ORDER BY created_at DESC;
SELECT * FROM payments ORDER BY created_at DESC;
SELECT * FROM notification_logs ORDER BY sent_at DESC;
SELECT * FROM outbox_events ORDER BY created_at DESC;
```

### Step 10 — View Kafka Topics

Open http://localhost:9000 (Kafka UI) to see:
- Topics: `subscription.created.v1`, `payment.completed.v1`, `subscription.activated.v1`
- Messages published during the demo flow

---

## 8. Dockerfile (per service)

Each service uses a **multi-stage build** to minimize the final image size:

```dockerfile
# Stage 1: Build
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# Cache Gradle dependencies layer
COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle
RUN ./gradlew dependencies --no-daemon

# Build the application
COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

# Stage 2: Runtime (minimal JRE image)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Non-root user for security
RUN addgroup -S sardis && adduser -S sardis -G sardis
USER sardis

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
```

**JVM flags explained:**
- `UseContainerSupport` — JVM respects Docker memory limits
- `MaxRAMPercentage=75.0` — Use 75% of container memory for heap

---

## 9. Docker Compose Configuration

```yaml
# compose.yaml
version: '3.8'

networks:
  sardis-network:
    driver: bridge

services:

  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    networks:
      - sardis-network

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: true
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    healthcheck:
      test: ["CMD", "kafka-topics", "--bootstrap-server", "localhost:9092", "--list"]
      interval: 30s
      timeout: 10s
      retries: 5
    networks:
      - sardis-network

  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    ports:
      - "9000:8080"
    environment:
      KAFKA_CLUSTERS_0_NAME: local
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:9092
    depends_on:
      - kafka
    networks:
      - sardis-network

  subscription-service:
    build:
      context: .
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: dev
      KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      JWT_SECRET: ${JWT_SECRET}
    depends_on:
      kafka:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "wget", "-q", "--spider", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
    networks:
      - sardis-network

  payment-service:
    build:
      context: ./payment-service
      dockerfile: Dockerfile
    ports:
      - "8081:8080"
    environment:
      SPRING_PROFILES_ACTIVE: dev
      KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      WEBHOOK_SECRET: ${WEBHOOK_SECRET}
    depends_on:
      kafka:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "wget", "-q", "--spider", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
    networks:
      - sardis-network

  notification-service:
    build:
      context: ./notification-service
      dockerfile: Dockerfile
    ports:
      - "8082:8080"
    environment:
      SPRING_PROFILES_ACTIVE: dev
      KAFKA_BOOTSTRAP_SERVERS: kafka:9092
    depends_on:
      kafka:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "wget", "-q", "--spider", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
    networks:
      - sardis-network
```

---

## 10. Monitoring & Health Checks

**Spring Boot Actuator endpoints (per service):**

| Endpoint | Description |
|----------|-------------|
| `GET /actuator/health` | Overall health (DB, Kafka, disk) |
| `GET /actuator/health/db` | Database connectivity |
| `GET /actuator/health/kafka` | Kafka connectivity |
| `GET /actuator/metrics` | All available metrics |
| `GET /actuator/metrics/http.server.requests` | Request rate + latency |
| `GET /actuator/prometheus` | Prometheus scrape endpoint |

**Key metrics to monitor in production:**
- `subscription.creation.rate` — subscriptions created per minute
- `payment.success.rate` — payment success percentage
- `renewal.batch.duration` — renewal scheduler run time
- `outbox.lag` — unprocessed outbox events count (alert if > 100)
- `http.server.requests[uri=/api/v1/subscriptions,status=5xx]` — error rate

---

## 11. Troubleshooting

**Services fail to connect to Kafka:**
```bash
# Check Kafka health
docker-compose ps kafka
# Wait for "healthy" status before starting services
docker-compose up -d zookeeper kafka
sleep 30
docker-compose up -d subscription-service payment-service notification-service
```

**H2 Console shows empty tables:**
```bash
# Check if seed data migration ran
docker-compose logs subscription-service | grep "Flyway"
# Should show: "Successfully applied 8 migrations"
```

**Payment webhook returns 401:**
```bash
# Compute correct HMAC
BODY='{"idempotencyKey":"test","status":"SUCCESS",...}'
echo -n "$BODY" | openssl dgst -sha256 -hmac "$WEBHOOK_SECRET"
# Use the output as X-Signature: sha256=<output>
```

**OutboxPoller not publishing events:**
```bash
# Check outbox table
# H2 Console: SELECT * FROM outbox_events WHERE processed = false;
# If records exist but not being published, check Kafka connectivity
docker-compose logs subscription-service | grep "OutboxPoller"
```
