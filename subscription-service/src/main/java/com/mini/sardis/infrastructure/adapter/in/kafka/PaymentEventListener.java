package com.mini.sardis.infrastructure.adapter.in.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mini.sardis.application.port.in.subscription.ActivateSubscriptionUseCase;
import com.mini.sardis.application.port.in.subscription.HandlePaymentFailedUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class PaymentEventListener {

    private final ActivateSubscriptionUseCase activateUseCase;
    private final HandlePaymentFailedUseCase handleFailedUseCase;
    private final ObjectMapper objectMapper;

    public PaymentEventListener(ActivateSubscriptionUseCase activateUseCase,
                                HandlePaymentFailedUseCase handleFailedUseCase,
                                ObjectMapper objectMapper) {
        this.activateUseCase = activateUseCase;
        this.handleFailedUseCase = handleFailedUseCase;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "payment.completed.v1", groupId = "subscription-payment-result")
    public void onPaymentCompleted(@Payload String payload,
                                   @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            UUID subscriptionId = UUID.fromString(node.get("subscriptionId").asText());
            String paymentType = node.has("paymentType") ? node.get("paymentType").asText() : "INITIAL";
            log.info("Payment completed for subscription={} type={}", subscriptionId, paymentType);
            activateUseCase.execute(subscriptionId, paymentType);
        } catch (Exception e) {
            log.error("Error processing payment.completed.v1: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "payment.failed.v1", groupId = "subscription-payment-result")
    public void onPaymentFailed(@Payload String payload,
                                @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            UUID subscriptionId = UUID.fromString(node.get("subscriptionId").asText());
            String paymentType = node.has("paymentType") ? node.get("paymentType").asText() : "INITIAL";
            String reason = node.has("reason") ? node.get("reason").asText() : "unknown";
            log.info("Payment failed for subscription={} type={} reason={}", subscriptionId, paymentType, reason);
            handleFailedUseCase.execute(subscriptionId, paymentType, reason);
        } catch (Exception e) {
            log.error("Error processing payment.failed.v1: {}", e.getMessage(), e);
        }
    }
}
