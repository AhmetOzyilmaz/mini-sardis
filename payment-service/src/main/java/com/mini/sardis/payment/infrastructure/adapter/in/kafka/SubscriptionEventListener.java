package com.mini.sardis.payment.infrastructure.adapter.in.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mini.sardis.payment.application.port.in.ProcessPaymentCommand;
import com.mini.sardis.payment.application.port.in.ProcessPaymentUseCase;
import com.mini.sardis.payment.domain.value.PaymentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class SubscriptionEventListener {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionEventListener.class);

    private final ProcessPaymentUseCase processPaymentUseCase;
    private final ObjectMapper objectMapper;

    public SubscriptionEventListener(ProcessPaymentUseCase processPaymentUseCase,
                                      ObjectMapper objectMapper) {
        this.processPaymentUseCase = processPaymentUseCase;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "subscription.created.v1", groupId = "payment-subscription-processor")
    public void onSubscriptionCreated(@Payload String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            UUID subscriptionId = UUID.fromString(node.get("subscriptionId").asText());
            UUID userId = UUID.fromString(node.get("userId").asText());
            BigDecimal amount = new BigDecimal(node.get("amount").asText());
            String currency = node.get("currency").asText();
            String idempotencyKey = node.get("idempotencyKey").asText();

            log.info("Received subscription.created.v1 for subscriptionId={}", subscriptionId);
            processPaymentUseCase.execute(new ProcessPaymentCommand(
                    subscriptionId, userId, idempotencyKey, amount, currency, PaymentType.INITIAL));
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

            log.info("Received renewal.requested.v1 for subscriptionId={}", subscriptionId);
            processPaymentUseCase.execute(new ProcessPaymentCommand(
                    subscriptionId, userId, idempotencyKey, amount, currency, PaymentType.RENEWAL));
        } catch (Exception e) {
            log.error("Error processing renewal.requested.v1: {}", e.getMessage(), e);
        }
    }
}
