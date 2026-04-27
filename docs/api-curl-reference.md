# API cURL Reference — mini-sardis

Import any `curl` command below directly into Postman via **Import → Raw Text**.

---

## Base URLs

| Service | Local | Docker Compose |
|---|---|---|
| Subscription | `http://localhost:8080` | `http://localhost:8080` |
| Payment | `http://localhost:8081` | `http://localhost:8081` |
| Notification | `http://localhost:8082` | `http://localhost:8082` |

---

## Variables used in examples

| Variable | Value |
|---|---|
| `SUBS_URL` | `http://localhost:8080` |
| `PAY_URL` | `http://localhost:8081` |
| `NOTIF_URL` | `http://localhost:8082` |
| `TOKEN` | JWT returned by `/auth/login` |
| `ADMIN_TOKEN` | JWT returned by `/auth/login` with admin credentials |

---

## 1. Auth — Subscription Service

### 1.1 Register

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john.doe@example.com",
    "password": "Password1",
    "fullName": "John Doe",
    "phoneNumber": "+905001234567"
  }'
```

**Response:** `201 Created` — no body

---

### 1.2 Login (User)

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user1@demo.com",
    "password": "Password1"
  }'
```

**Response:** `200 OK`
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "role": "ROLE_USER"
}
```

---

### 1.3 Login (Admin)

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@demo.com",
    "password": "Admin1234"
  }'
```

**Response:** `200 OK` — same structure as 1.2, `role` will be `ROLE_ADMIN`

---

## 2. Plans — Subscription Service

### 2.1 List All Plans

```bash
curl -X GET http://localhost:8080/api/v1/plans
```

**Response:** `200 OK`
```json
[
  {
    "id": "a0000001-0000-0000-0000-000000000001",
    "name": "Basic",
    "description": "Basic plan",
    "price": 49.99,
    "currency": "TRY",
    "durationDays": 30,
    "trialDays": 0
  }
]
```

---

### 2.2 Get Plan by ID

```bash
curl -X GET http://localhost:8080/api/v1/plans/a0000001-0000-0000-0000-000000000001
```

**Response:** `200 OK` — single plan object

---

## 3. Subscriptions — Subscription Service

### 3.1 Create Subscription (Credit Card, no promo)

```bash
curl -X POST http://localhost:8080/api/v1/subscriptions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "planId": "a0000001-0000-0000-0000-000000000001",
    "paymentMethod": "CREDIT_CARD"
  }'
```

**Response:** `202 Accepted`
```json
{
  "id": "...",
  "userId": "...",
  "planId": "a0000001-0000-0000-0000-000000000001",
  "status": "PENDING",
  "startDate": "2026-04-26",
  "nextRenewalDate": "2026-05-26",
  "createdAt": "2026-04-26T10:00:00",
  "amount": 49.99,
  "discountAmount": 0.00,
  "finalAmount": 49.99
}
```

---

### 3.2 Create Subscription with Promo Code

```bash
curl -X POST http://localhost:8080/api/v1/subscriptions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "planId": "a0000001-0000-0000-0000-000000000001",
    "paymentMethod": "CREDIT_CARD",
    "promoCode": "SAVE10PCT"
  }'
```

---

### 3.3 Create Subscription with Bank Transfer

```bash
curl -X POST http://localhost:8080/api/v1/subscriptions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "planId": "a0000001-0000-0000-0000-000000000001",
    "paymentMethod": "BANK_TRANSFER"
  }'
```

---

### 3.4 Get My Subscriptions

```bash
curl -X GET http://localhost:8080/api/v1/subscriptions/my \
  -H "Authorization: Bearer $TOKEN"
```

**Response:** `200 OK` — array of subscription objects

---

### 3.5 Get Subscription by ID

```bash
curl -X GET http://localhost:8080/api/v1/subscriptions/{id}
```

---

### 3.6 Get All Subscriptions (Admin only)

```bash
curl -X GET http://localhost:8080/api/v1/subscriptions \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

**Response:** `200 OK` — array of all subscriptions  
**Without admin token:** `403 Forbidden`

---

### 3.7 Cancel Subscription

```bash
curl -X DELETE "http://localhost:8080/api/v1/subscriptions/{id}?reason=user_request" \
  -H "Authorization: Bearer $TOKEN"
```

**Response:** `204 No Content`

---

## 4. Promo Codes — Subscription Service

### 4.1 Create Promo Code (Admin only)

**Percentage discount, no restrictions:**
```bash
curl -X POST http://localhost:8080/api/v1/admin/promo-codes \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "code": "SAVE10PCT",
    "discountType": "PERCENTAGE",
    "discountValue": 10,
    "maxUses": 100
  }'
```

**Response:** `201 Created`
```json
{
  "id": "...",
  "code": "SAVE10PCT",
  "discountType": "PERCENTAGE",
  "discountValue": 10,
  "maxUses": 100,
  "currentUses": 0,
  "active": true,
  "validFrom": null,
  "validTo": null,
  "applicableMonths": null
}
```

---

**Fixed amount, valid date range:**
```bash
curl -X POST http://localhost:8080/api/v1/admin/promo-codes \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "code": "FLAT20TRY",
    "discountType": "FIXED",
    "discountValue": 20,
    "maxUses": 50,
    "validFrom": "2026-04-26T00:00:00",
    "validTo": "2026-12-31T23:59:59"
  }'
```

---

**12-month plan only:**
```bash
curl -X POST http://localhost:8080/api/v1/admin/promo-codes \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "code": "ANNUAL20",
    "discountType": "PERCENTAGE",
    "discountValue": 20,
    "maxUses": 50,
    "applicableMonths": [12]
  }'
```

---

### 4.2 Validate Promo Code

**Basic validation:**
```bash
curl -X GET http://localhost:8080/api/v1/promo-codes/SAVE10PCT/validate
```

**With plan duration check:**
```bash
curl -X GET "http://localhost:8080/api/v1/promo-codes/ANNUAL20/validate?durationMonths=12"
```

**Response:** `200 OK`
```json
{
  "code": "SAVE10PCT",
  "discountType": "PERCENTAGE",
  "discountValue": 10,
  "maxUses": 100,
  "currentUses": 0,
  "valid": true,
  "message": "Promo code is valid",
  "applicableMonths": null
}
```

---

## 5. Payments — Payment Service

### 5.1 Get Payment History by Subscription

```bash
curl -X GET http://localhost:8081/api/v1/payments/subscription/{subscriptionId}
```

**Response:** `200 OK`
```json
[
  {
    "id": "...",
    "subscriptionId": "...",
    "amount": 49.99,
    "currency": "TRY",
    "status": "SUCCESS",
    "type": "INITIAL",
    "paymentMethod": "CREDIT_CARD",
    "externalRef": "ext-ref-xyz",
    "failureReason": null,
    "retryCount": 0,
    "createdAt": "2026-04-26T10:00:00"
  }
]
```

---

### 5.2 Payment Webhook — Missing Signature

```bash
curl -X POST http://localhost:8081/api/v1/payments/webhook \
  -H "Content-Type: application/json" \
  -d '{
    "idempotencyKey": "key-001",
    "success": true
  }'
```

**Response:** `401 Unauthorized`

---

### 5.3 Payment Webhook — Valid Signature

Generate the HMAC-SHA256 signature (Linux/Mac):
```bash
SECRET="dev-webhook-secret-key"
BODY='{"idempotencyKey":"key-001","externalRef":"ext-ref-123","success":true,"failureReason":null}'
SIG=$(echo -n "$BODY" | openssl dgst -sha256 -hmac "$SECRET" | awk '{print $2}')

curl -X POST http://localhost:8081/api/v1/payments/webhook \
  -H "Content-Type: application/json" \
  -H "X-Signature: $SIG" \
  -d "$BODY"
```

Generate signature (Windows PowerShell):
```powershell
$secret = "dev-webhook-secret-key"
$body   = '{"idempotencyKey":"key-001","externalRef":"ext-ref-123","success":true,"failureReason":null}'
$hmac   = [System.Security.Cryptography.HMACSHA256]::new([System.Text.Encoding]::UTF8.GetBytes($secret))
$sig    = [System.BitConverter]::ToString($hmac.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($body))) -replace '-',''
$sig    = $sig.ToLower()
Write-Output "X-Signature: $sig"
```

---

### 5.4 Payment Webhook — Success with Known Key

```bash
curl -X POST http://localhost:8081/api/v1/payments/webhook \
  -H "Content-Type: application/json" \
  -H "X-Signature: <computed-hmac>" \
  -d '{
    "idempotencyKey": "<existing-idempotency-key>",
    "externalRef": "ext-ref-ok",
    "success": true,
    "failureReason": null
  }'
```

**Response:** `200 OK`

---

### 5.5 Payment Webhook — Failed Payment

```bash
curl -X POST http://localhost:8081/api/v1/payments/webhook \
  -H "Content-Type: application/json" \
  -H "X-Signature: <computed-hmac>" \
  -d '{
    "idempotencyKey": "<existing-idempotency-key>",
    "externalRef": "ext-ref-fail",
    "success": false,
    "failureReason": "insufficient_funds"
  }'
```

**Response:** `200 OK`

---

## 6. Notifications — Notification Service

### 6.1 Get Notifications by User

```bash
curl -X GET http://localhost:8082/api/v1/notifications/user/{userId}
```

**Response:** `200 OK`
```json
[
  {
    "id": "...",
    "userId": "...",
    "channel": "EMAIL",
    "subject": "Subscription Activated",
    "body": "Your subscription has been activated.",
    "success": true,
    "sentAt": "2026-04-26T10:00:05"
  }
]
```

---

### 6.2 Get All Notifications

```bash
curl -X GET http://localhost:8082/api/v1/notifications
```

**Response:** `200 OK` — array of all notification log entries

---

## 7. End-to-End Flow Example

Full subscription lifecycle — copy and run in order:

```bash
# 1. Login
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user1@demo.com","password":"Password1"}' | jq -r '.token')

# 2. Create subscription
SUBS=$(curl -s -X POST http://localhost:8080/api/v1/subscriptions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"planId":"a0000001-0000-0000-0000-000000000001","paymentMethod":"CREDIT_CARD"}')
echo $SUBS | jq .

SUBS_ID=$(echo $SUBS | jq -r '.id')
IDEM_KEY=$(curl -s http://localhost:8081/api/v1/payments/subscription/$SUBS_ID | jq -r '.[0].idempotencyKey')

# 3. Simulate payment webhook success
SECRET="dev-webhook-secret-key"
BODY="{\"idempotencyKey\":\"$IDEM_KEY\",\"externalRef\":\"ext-ok\",\"success\":true,\"failureReason\":null}"
SIG=$(echo -n "$BODY" | openssl dgst -sha256 -hmac "$SECRET" | awk '{print $2}')

curl -X POST http://localhost:8081/api/v1/payments/webhook \
  -H "Content-Type: application/json" \
  -H "X-Signature: $SIG" \
  -d "$BODY"

# 4. Check subscription status (should be ACTIVE after Kafka event)
sleep 2
curl -s http://localhost:8080/api/v1/subscriptions/$SUBS_ID | jq .

# 5. Check notification log
curl -s http://localhost:8082/api/v1/notifications/user/$(echo $SUBS | jq -r '.userId') | jq .

# 6. Cancel subscription
curl -X DELETE "http://localhost:8080/api/v1/subscriptions/$SUBS_ID?reason=user_request" \
  -H "Authorization: Bearer $TOKEN"
```

> Requires `jq` for JSON parsing. Install with `brew install jq` (Mac) or `choco install jq` (Windows).

---

## 8. Postman Import Tips

1. Open Postman → **Import** → **Raw text**
2. Paste any curl command above
3. Postman parses the method, URL, headers, and body automatically
4. Set `TOKEN` and `ADMIN_TOKEN` as **Collection Variables** after running the login requests, then reference them as `{{TOKEN}}` and `{{ADMIN_TOKEN}}` in Authorization headers
5. For the webhook signature, add a **Pre-request Script** in Postman:

```javascript
const secret = "dev-webhook-secret-key";
const body = JSON.stringify(pm.request.body.raw ? JSON.parse(pm.request.body.raw) : {});
const encoder = new TextEncoder();
const keyData = encoder.encode(secret);
const bodyData = encoder.encode(body);

crypto.subtle.importKey("raw", keyData, { name: "HMAC", hash: "SHA-256" }, false, ["sign"])
  .then(key => crypto.subtle.sign("HMAC", key, bodyData))
  .then(sig => {
    const hex = Array.from(new Uint8Array(sig)).map(b => b.toString(16).padStart(2, "0")).join("");
    pm.request.headers.upsert({ key: "X-Signature", value: hex });
  });
```

---

## 9. HTTP Status Code Reference

| Code | Meaning |
|---|---|
| `200 OK` | Successful GET / webhook processed |
| `201 Created` | Resource created (register, promo code, offer) |
| `202 Accepted` | Subscription creation or refund request accepted (async processing) |
| `204 No Content` | Successful DELETE |
| `400 Bad Request` | Validation error or unknown idempotency key |
| `401 Unauthorized` | Missing / invalid token or webhook signature |
| `403 Forbidden` | Authenticated but insufficient role (needs ADMIN) |
| `404 Not Found` | Resource does not exist |
| `409 Conflict` | Duplicate unique value or invalid state transition |

---

## 10. New Features — Subscription Service

### 10.1 Cancel at Period End

Defers cancellation until the end of the current billing period (subscription stays ACTIVE until `nextRenewalDate`):

```bash
curl -X DELETE "http://localhost:8080/api/v1/subscriptions/$SUBS_ID?cancelAtPeriodEnd=true&reason=downgrade" \
  -H "Authorization: Bearer $TOKEN"
```

**Response:** `204 No Content`

> To cancel immediately (default), omit `cancelAtPeriodEnd` or set it to `false`.

---

### 10.2 Reactivate Subscription

Reactivates a `SUSPENDED` or `GRACE_PERIOD` subscription:

```bash
curl -X POST http://localhost:8080/api/v1/subscriptions/$SUBS_ID/reactivate \
  -H "Authorization: Bearer $TOKEN"
```

**Response:** `200 OK`
```json
{
  "id": "...",
  "status": "ACTIVE",
  "gracePeriodEndDate": null
}
```

---

### 10.3 Request Refund

Initiates an async refund for a subscription (processed via Kafka saga):

```bash
curl -X POST "http://localhost:8080/api/v1/subscriptions/$SUBS_ID/refund?reason=unsatisfied" \
  -H "Authorization: Bearer $TOKEN"
```

**Response:** `202 Accepted` — no body; check `refund.completed.v1` notification or payment-service logs.

---

### 10.4 Transaction History — By Subscription

All lifecycle events recorded for a specific subscription:

```bash
curl -X GET http://localhost:8080/api/v1/subscriptions/$SUBS_ID/transactions \
  -H "Authorization: Bearer $TOKEN"
```

**Response:** `200 OK`
```json
[
  {
    "eventId": "...",
    "subscriptionId": "...",
    "eventType": "subscription.created.v1",
    "payloadSummary": "{\"subscriptionId\":\"...\",\"planName\":\"Basic\",...}",
    "occurredAt": "2026-04-27T10:00:00"
  }
]
```

---

### 10.5 Transaction History — My Subscriptions

All subscription events across all of the authenticated user's subscriptions:

```bash
curl -X GET http://localhost:8080/api/v1/transactions/my \
  -H "Authorization: Bearer $TOKEN"
```

**Response:** `200 OK` — same structure as 10.4

---

### 10.6 Assign Promo Code to Users (Admin)

Assigns a promo code to one or more specific users:

```bash
curl -X POST http://localhost:8080/api/v1/admin/promo-codes/SAVE10PCT/assign \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "userIds": [
      "550e8400-e29b-41d4-a716-446655440001",
      "550e8400-e29b-41d4-a716-446655440002"
    ]
  }'
```

**Response:** `200 OK` — no body

---

### 10.7 My Assigned Promo Codes

Lists all promo codes assigned to the authenticated user:

```bash
curl -X GET http://localhost:8080/api/v1/my-promo-codes \
  -H "Authorization: Bearer $TOKEN"
```

**Response:** `200 OK`
```json
[
  {
    "id": "...",
    "userId": "...",
    "code": "SAVE10PCT",
    "assignedAt": "2026-04-27T09:00:00",
    "used": false,
    "usedAt": null
  }
]
```

---

### 10.8 Get Eligible Offers

Returns personalized offers the authenticated user is eligible for:

```bash
curl -X GET http://localhost:8080/api/v1/offers \
  -H "Authorization: Bearer $TOKEN"
```

**Response:** `200 OK`
```json
[
  {
    "id": "...",
    "name": "Spring Upgrade Deal",
    "description": "Upgrade to Premium at 20% off",
    "planId": "a0000001-0000-0000-0000-000000000002",
    "promoCodeId": "...",
    "targetType": "ALL_USERS",
    "validFrom": "2026-04-01T00:00:00",
    "validTo": "2026-06-30T23:59:59",
    "active": true,
    "createdAt": "2026-04-01T08:00:00"
  }
]
```

---

### 10.9 Get Offer by ID

```bash
curl -X GET http://localhost:8080/api/v1/offers/$OFFER_ID \
  -H "Authorization: Bearer $TOKEN"
```

**Response:** `200 OK` — single offer object

---

### 10.10 Create Offer (Admin)

```bash
curl -X POST http://localhost:8080/api/v1/admin/offers \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "name": "Spring Upgrade Deal",
    "description": "Upgrade to Premium at 20% off",
    "planId": "a0000001-0000-0000-0000-000000000002",
    "promoCodeId": null,
    "targetType": "ALL_USERS",
    "validFrom": "2026-04-01T00:00:00",
    "validTo": "2026-06-30T23:59:59"
  }'
```

**Response:** `201 Created`

> `targetType` values: `ALL_USERS`, `SPECIFIC_USER` (requires `targetUserId`), `PLAN_UPGRADE` (requires `targetPlanId`).

---

## 11. End-to-End Flow — Grace Period & Refund

Demonstrates renewal failure → grace period → refund request:

```bash
# 1. Login
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user1@demo.com","password":"Password1"}' | jq -r '.token')

# 2. Create + activate subscription (see Section 7)
SUBS_ID="<activated-subscription-id>"

# 3. Simulate renewal payment failure
#    (wait for RenewalScheduler or trigger by setting nextRenewalDate to today in DB)
#    After failure, subscription status becomes GRACE_PERIOD

# 4. Check status — should be GRACE_PERIOD
curl -s http://localhost:8080/api/v1/subscriptions/$SUBS_ID | jq .status

# 5. Request refund while in GRACE_PERIOD
curl -X POST "http://localhost:8080/api/v1/subscriptions/$SUBS_ID/refund?reason=renewal_failed" \
  -H "Authorization: Bearer $TOKEN"
# → 202 Accepted; payment-service processes refund.requested.v1 async

# 6. Check transaction history
curl -s http://localhost:8080/api/v1/subscriptions/$SUBS_ID/transactions \
  -H "Authorization: Bearer $TOKEN" | jq .

# 7. Reactivate after resolving payment issue
curl -X POST http://localhost:8080/api/v1/subscriptions/$SUBS_ID/reactivate \
  -H "Authorization: Bearer $TOKEN"
```
