package com.mini.sardis.payment.infrastructure.adapter.in.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mini.sardis.payment.BaseComponentTest;
import com.mini.sardis.payment.domain.value.PaymentMethod;
import com.mini.sardis.payment.domain.value.PaymentStatus;
import com.mini.sardis.payment.domain.value.PaymentType;
import com.mini.sardis.payment.infrastructure.adapter.out.jpa.JpaPaymentRepository;
import com.mini.sardis.payment.infrastructure.adapter.out.jpa.PaymentJpaEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentApiComponentTest extends BaseComponentTest {

    @Autowired JpaPaymentRepository paymentRepo;
    @Autowired ObjectMapper objectMapper;

    @Value("${app.webhook.secret:dev-webhook-secret-key}")
    String webhookSecret;

    @Test
    void getPaymentHistory_emptyForUnknownSubscription() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/payments/subscription/" + UUID.randomUUID(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("[]");
    }

    @Test
    void getPaymentHistory_returnsStoredPayments() throws Exception {
        UUID subscriptionId = UUID.randomUUID();
        paymentRepo.save(buildEntity(subscriptionId, PaymentStatus.SUCCESS, PaymentMethod.CREDIT_CARD));

        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/payments/subscription/" + subscriptionId, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.size()).isEqualTo(1);
        assertThat(body.get(0).get("status").asText()).isEqualTo("SUCCESS");
        assertThat(body.get(0).get("paymentMethod").asText()).isEqualTo("CREDIT_CARD");
        assertThat(body.get(0).get("amount").decimalValue()).isEqualByComparingTo("99.99");
    }

    @Test
    void getPaymentHistory_multipleMethodsTrackedIndependently() throws Exception {
        UUID subscriptionId = UUID.randomUUID();
        paymentRepo.save(buildEntity(subscriptionId, PaymentStatus.FAILED, PaymentMethod.CREDIT_CARD));
        paymentRepo.save(buildEntity(subscriptionId, PaymentStatus.SUCCESS, PaymentMethod.BANK_TRANSFER));

        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/payments/subscription/" + subscriptionId, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.size()).isEqualTo(2);
    }

    @Test
    void getPaymentHistory_differentSubscriptions_isolatedResults() throws Exception {
        UUID subA = UUID.randomUUID();
        UUID subB = UUID.randomUUID();
        paymentRepo.save(buildEntity(subA, PaymentStatus.SUCCESS, PaymentMethod.DIGITAL_WALLET));
        paymentRepo.save(buildEntity(subB, PaymentStatus.SUCCESS, PaymentMethod.DEBIT_CARD));

        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/payments/subscription/" + subA, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.size()).isEqualTo(1);
        assertThat(body.get(0).get("paymentMethod").asText()).isEqualTo("DIGITAL_WALLET");
    }

    @Test
    void webhook_missingSignature_returns401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>("{\"idempotencyKey\":\"key-1\",\"success\":true}", headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/payments/webhook", HttpMethod.POST, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void webhook_invalidSignature_returns401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Signature", "invalid-signature-value");
        HttpEntity<String> request = new HttpEntity<>("{\"idempotencyKey\":\"key-1\",\"success\":true}", headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/payments/webhook", HttpMethod.POST, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void webhook_validSignatureUnknownKey_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(
                new WebhookRequest("nonexistent-key", "ext-ref", true, null));
        String signature = computeHmac(body);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Signature", signature);
        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/payments/webhook", HttpMethod.POST, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void webhook_validSignatureKnownPendingKey_returns200() throws Exception {
        UUID subscriptionId = UUID.randomUUID();
        String idempotencyKey = "webhook-test-" + UUID.randomUUID();
        PaymentJpaEntity entity = buildEntity(subscriptionId, PaymentStatus.PENDING, PaymentMethod.CREDIT_CARD);
        entity.setIdempotencyKey(idempotencyKey);
        paymentRepo.save(entity);

        String body = objectMapper.writeValueAsString(
                new WebhookRequest(idempotencyKey, "ext-ref-ok", true, null));
        String signature = computeHmac(body);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Signature", signature);
        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/payments/webhook", HttpMethod.POST, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private PaymentJpaEntity buildEntity(UUID subscriptionId, PaymentStatus status, PaymentMethod method) {
        return PaymentJpaEntity.builder()
                .id(UUID.randomUUID())
                .subscriptionId(subscriptionId)
                .userId(UUID.randomUUID())
                .idempotencyKey("test-" + UUID.randomUUID())
                .amount(BigDecimal.valueOf(99.99))
                .currency("TRY")
                .status(status)
                .type(PaymentType.INITIAL)
                .paymentMethod(method)
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private String computeHmac(String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }
}
