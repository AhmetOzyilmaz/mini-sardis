package com.mini.sardis.payment.infrastructure.adapter.in.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mini.sardis.payment.application.port.in.ProcessPaymentCommand;
import com.mini.sardis.payment.application.port.in.ProcessPaymentUseCase;
import com.mini.sardis.payment.application.port.in.ProcessRefundCommand;
import com.mini.sardis.payment.application.port.in.ProcessRefundUseCase;
import com.mini.sardis.payment.domain.value.PaymentMethod;
import com.mini.sardis.payment.domain.value.PaymentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionEventListener {

    private final ProcessPaymentUseCase processPaymentUseCase;
    private final ProcessRefundUseCase processRefundUseCase;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "subscription.created.v1", groupId = "payment-subscription-processor")
    public void onSubscriptionCreated(@Payload String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            UUID subscriptionId = UUID.fromString(node.get("subscriptionId").asText());
            UUID userId = UUID.fromString(node.get("userId").asText());
            BigDecimal amount = new BigDecimal(node.get("amount").asText());
            BigDecimal finalAmount = node.has("finalAmount")
                    ? new BigDecimal(node.get("finalAmount").asText())
                    : amount;
            String currency = node.get("currency").asText();
            String idempotencyKey = node.get("idempotencyKey").asText();
            PaymentMethod paymentMethod = parsePaymentMethod(node.path("paymentMethod").asText(null));

            log.info("Received subscription.created.v1 for subscriptionId={} method={}", subscriptionId, paymentMethod);
            processPaymentUseCase.execute(new ProcessPaymentCommand(
                    subscriptionId, userId, idempotencyKey, finalAmount, currency,
                    PaymentType.INITIAL, paymentMethod));
        } catch (Exception e) {
            log.error("Error processing subscription.created.v1: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "renewal.requested.v1", groupId = "payment-renewal-processor")
    public void onRenewalRequested(@Payload String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            UUID subscriptionId = UUID.fromString(node.get("subscriptionId").asText());
            UUID userId = UUID.fromString(node.get("userId").asText());
            BigDecimal amount = new BigDecimal(node.get("amount").asText());
            String currency = node.get("currency").asText();
            String idempotencyKey = node.get("idempotencyKey").asText();

            PaymentMethod paymentMethod = parsePaymentMethod(node.path("paymentMethod").asText(null));
            log.info("Received renewal.requested.v1 for subscriptionId={} method={}", subscriptionId, paymentMethod);
            processPaymentUseCase.execute(new ProcessPaymentCommand(
                    subscriptionId, userId, idempotencyKey, amount, currency,
                    PaymentType.RENEWAL, paymentMethod));
        } catch (Exception e) {
            log.error("Error processing renewal.requested.v1: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "refund.requested.v1", groupId = "payment-refund-processor")
    public void onRefundRequested(@Payload String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            UUID subscriptionId = UUID.fromString(node.get("subscriptionId").asText());
            UUID userId = UUID.fromString(node.get("userId").asText());
            String reason = node.path("reason").asText("user_request");

            log.info("Received refund.requested.v1 for subscriptionId={}", subscriptionId);
            processRefundUseCase.execute(new ProcessRefundCommand(subscriptionId, userId, reason));
        } catch (Exception e) {
            log.error("Error processing refund.requested.v1: {}", e.getMessage(), e);
        }
    }

    private PaymentMethod parsePaymentMethod(String value) {
        if (value == null || value.isBlank()) return PaymentMethod.CREDIT_CARD;
        try {
            return PaymentMethod.valueOf(value);
        } catch (IllegalArgumentException e) {
            return PaymentMethod.CREDIT_CARD;
        }
    }
}
