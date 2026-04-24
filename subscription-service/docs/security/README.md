# Security Design — Subscription Management System

## Overview

Authentication and authorization are implemented as a stateless JWT Bearer mechanism.
Every request is validated independently — no server-side session state.

The security layer follows the hexagonal architecture contract:

- **Domain layer** — knows nothing about security
- **Application layer** — uses `AuthenticationPort` (port) to read the current principal
- **Infrastructure layer** — owns `JwtTokenProvider`, `JwtAuthenticationFilter`, `SecurityConfig`

---

## Authentication Flow

```
POST /api/v1/auth/register
  Body: { email, password, fullName, phoneNumber }
  → RegisterUserService validates uniqueness, BCrypt-hashes password, saves User
  → 201 Created (no token — user must log in separately)

POST /api/v1/auth/login
  Body: { email, password }
  → LoginService verifies BCrypt hash via PasswordEncoder
  → JwtTokenProvider.generateToken(user) signs HS512 JWT
  → 200 OK { token, tokenType: "Bearer", expiresIn, userId, role }

Every subsequent request:
  Authorization: Bearer <token>
  → JwtAuthenticationFilter extracts + validates token
  → Sets UsernamePasswordAuthenticationToken in SecurityContextHolder
  → Spring Security enforces access rules
```

---

## JWT Token

| Property | Value |
|----------|-------|
| Algorithm | HS512 (HMAC-SHA-512) |
| Library | jjwt 0.12.x |
| TTL | 24 hours (configurable via `app.jwt.expiration-ms`) |
| Refresh tokens | Not supported — re-login after expiry |
| Claims | `sub` (userId UUID), `email`, `role` |

**Secret key configuration:**
```properties
# application.properties
app.jwt.secret=${APP_JWT_SECRET:dev-secret-key-must-be-at-least-32-characters-long!!}
app.jwt.expiration-ms=${APP_JWT_EXPIRATION_MS:86400000}
```

Secret is validated at startup by `JwtProperties` (`@Validated @ConfigurationProperties`).
A blank or short secret causes a startup failure — fail-fast by design.

---

## Authorization Model

### Roles

| Role | Description |
|------|-------------|
| `ROLE_USER` | Regular subscriber — can manage own subscriptions |
| `ROLE_ADMIN` | Platform admin — can view all data, deactivate plans |

### Endpoint Access Rules (SecurityConfig)

| Pattern | Method | Access |
|---------|--------|--------|
| `/api/v1/auth/**` | POST | Public |
| `/api/v1/plans`, `/api/v1/plans/**` | GET | Public |
| `/api/v1/payments/webhook` | POST | Public (HMAC-verified) |
| `/actuator/health` | GET | Public |
| `/actuator/**` | ANY | `ROLE_ADMIN` only |
| `/swagger-ui/**`, `/v3/api-docs/**` | ANY | Public |
| `/h2-console/**` | ANY | Public (dev only) |
| Everything else | ANY | Authenticated |

### Resource-Level Authorization

Resource ownership checks happen in the **use case layer**, not the controller.

```java
// Example pattern — in CancelSubscriptionUseCase implementation:
UUID callerId = authenticationPort.currentUserId();
Subscription sub = subscriptionRepository.findById(id).orElseThrow(...);

if (!authenticationPort.isAdmin() && !sub.getUserId().equals(callerId)) {
    throw new AccessDeniedException("Not your subscription");
}
```

`AuthenticationPort` is injected from `SpringSecurityAuthenticationAdapter` which reads
the `SecurityContextHolder`. This keeps use case code free of Spring Security imports.

---

## Webhook Security

`POST /api/v1/payments/webhook` does not use JWT (the payment provider is not a user).
It is protected by HMAC-SHA256 signature verification:

```
X-Signature: sha256=<HMAC-SHA256(body, APP_WEBHOOK_SECRET)>
```

`WebhookSignatureVerifier` uses `MessageDigest.isEqual` (constant-time comparison) to
prevent timing attacks. Requests with a missing or invalid signature are rejected with 401.

Secret configuration:
```properties
app.webhook.secret=${APP_WEBHOOK_SECRET:dev-webhook-secret-key}
```

---

## Password Security

- Algorithm: BCrypt with cost factor 12
- Encoder: `BCryptPasswordEncoder(12)` defined as a `@Bean` in `SecurityConfig`
- Password requirements (enforced by `RegisterRequest` validation):
  - Minimum 8 characters
  - At least one uppercase letter
  - At least one digit
- Passwords are **never logged** — only `userId` appears in log MDC

---

## Error Responses

All security errors return `application/problem+json` (RFC 7807):

```json
{
  "type": "https://sardis.io/errors/unauthorized",
  "title": "Unauthorized",
  "status": 401,
  "detail": "Authentication required",
  "instance": "/api/v1/subscriptions/123"
}
```

| HTTP Status | Trigger |
|-------------|---------|
| 401 | Missing or invalid JWT; invalid webhook signature |
| 403 | Valid JWT but insufficient role |

Internal class names and stack traces are never included in error responses.

---

## CORS

Configured in `SecurityConfig.corsConfigurationSource()`:

| Setting | Value |
|---------|-------|
| Allowed origins | `app.cors.allowed-origins` (comma-separated, env-configurable) |
| Allowed methods | GET, POST, PUT, DELETE, OPTIONS |
| Allowed headers | Authorization, Content-Type, X-Signature |
| Credentials | Allowed |

Default dev value: `http://localhost:3000`

---

## Key Files

| File | Layer | Purpose |
|------|-------|---------|
| `application/port/in/security/AuthenticationPort.java` | Application (port) | Principal access interface for use cases |
| `application/port/out/TokenGeneratorPort.java` | Application (port) | Token generation interface (hexagonal boundary) |
| `infrastructure/security/JwtTokenProvider.java` | Infrastructure | jjwt token generation + validation; implements `TokenGeneratorPort` |
| `infrastructure/security/JwtAuthenticationFilter.java` | Infrastructure | `OncePerRequestFilter` — extracts Bearer token, populates SecurityContext |
| `infrastructure/security/SpringSecurityAuthenticationAdapter.java` | Infrastructure | Implements `AuthenticationPort` using `SecurityContextHolder` |
| `infrastructure/security/WebhookSignatureVerifier.java` | Infrastructure | HMAC-SHA256 webhook body verification |
| `infrastructure/security/ProblemJsonAuthEntryPoint.java` | Infrastructure | 401 `application/problem+json` response |
| `infrastructure/security/ProblemJsonAccessDeniedHandler.java` | Infrastructure | 403 `application/problem+json` response |
| `infrastructure/config/SecurityConfig.java` | Infrastructure | `SecurityFilterChain`, `PasswordEncoder`, CORS beans |
| `infrastructure/config/JwtProperties.java` | Infrastructure | `@ConfigurationProperties` with `@Validated` startup check |

---

## Demo Credentials (H2 / dev profile)

Seeded by `DataInitializer` on startup:

| Email | Password | Role |
|-------|----------|------|
| `admin@demo.com` | `Admin1234` | ADMIN |
| `user1@demo.com` | `Password1` | USER |
| `user2@demo.com` | `Password1` | USER |
| `user3@demo.com` | `Password1` | USER |

Login example:
```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"user1@demo.com","password":"Password1"}' | jq .
```
