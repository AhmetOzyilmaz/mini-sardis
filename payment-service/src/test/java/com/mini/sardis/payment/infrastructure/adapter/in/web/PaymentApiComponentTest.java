package com.mini.sardis.payment.infrastructure.adapter.in.web;

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
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
class PaymentApiComponentTest extends BaseComponentTest {

    @Autowired JpaPaymentRepository paymentRepo;
    @Autowired ObjectMapper objectMapper;

    @Value("${app.webhook.secret:dev-webhook-secret-key}")
    String webhookSecret;

    @Test
    void getPaymentHistory_emptyForUnknownSubscription() throws Exception {
        mockMvc.perform(get("/api/v1/payments/subscription/" + UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getPaymentHistory_returnsStoredPayments() throws Exception {
        UUID subscriptionId = UUID.randomUUID();
        paymentRepo.save(buildEntity(subscriptionId, PaymentStatus.SUCCESS, PaymentMethod.CREDIT_CARD));

        mockMvc.perform(get("/api/v1/payments/subscription/" + subscriptionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$[0].paymentMethod").value("CREDIT_CARD"))
                .andExpect(jsonPath("$[0].amount").value(99.99));
    }

    @Test
    void getPaymentHistory_multipleMethodsTrackedIndependently() throws Exception {
        UUID subscriptionId = UUID.randomUUID();
        paymentRepo.save(buildEntity(subscriptionId, PaymentStatus.FAILED, PaymentMethod.CREDIT_CARD));
        paymentRepo.save(buildEntity(subscriptionId, PaymentStatus.SUCCESS, PaymentMethod.BANK_TRANSFER));

        mockMvc.perform(get("/api/v1/payments/subscription/" + subscriptionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getPaymentHistory_differentSubscriptions_isolatedResults() throws Exception {
        UUID subA = UUID.randomUUID();
        UUID subB = UUID.randomUUID();
        paymentRepo.save(buildEntity(subA, PaymentStatus.SUCCESS, PaymentMethod.DIGITAL_WALLET));
        paymentRepo.save(buildEntity(subB, PaymentStatus.SUCCESS, PaymentMethod.DEBIT_CARD));

        mockMvc.perform(get("/api/v1/payments/subscription/" + subA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].paymentMethod").value("DIGITAL_WALLET"));
    }

    @Test
    void webhook_missingSignature_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/payments/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotencyKey\":\"key-1\",\"success\":true}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void webhook_invalidSignature_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/payments/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Signature", "invalid-signature-value")
                .content("{\"idempotencyKey\":\"key-1\",\"success\":true}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void webhook_validSignatureUnknownKey_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(
                new WebhookRequest("nonexistent-key", "ext-ref", true, null));
        String signature = computeHmac(body);

        mockMvc.perform(post("/api/v1/payments/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Signature", signature)
                .content(body))
                .andExpect(status().isBadRequest());
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

        mockMvc.perform(post("/api/v1/payments/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Signature", signature)
                .content(body))
                .andExpect(status().isOk());
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
