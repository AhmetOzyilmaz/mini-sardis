package com.mini.sardis.infrastructure.adapter.in.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefundEventListener {

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "refund.completed.v1", groupId = "subscription-refund-result")
    public void onRefundCompleted(@Payload String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            UUID subscriptionId = UUID.fromString(node.get("subscriptionId").asText());
            UUID refundId = UUID.fromString(node.get("refundId").asText());
            log.info("Refund completed: refundId={} subscriptionId={}", refundId, subscriptionId);
        } catch (Exception e) {
            log.error("Error processing refund.completed.v1: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "refund.failed.v1", groupId = "subscription-refund-result")
    public void onRefundFailed(@Payload String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            UUID subscriptionId = UUID.fromString(node.get("subscriptionId").asText());
            String reason = node.path("reason").asText();
            log.warn("Refund failed: subscriptionId={} reason={}", subscriptionId, reason);
        } catch (Exception e) {
            log.error("Error processing refund.failed.v1: {}", e.getMessage(), e);
        }
    }
}
