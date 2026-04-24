package com.mini.sardis.notification.infrastructure.adapter.in.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mini.sardis.notification.application.port.in.SendNotificationCommand;
import com.mini.sardis.notification.application.port.in.SendNotificationUseCase;
import com.mini.sardis.notification.domain.value.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SubscriptionEventListener {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionEventListener.class);

    private final SendNotificationUseCase sendNotificationUseCase;
    private final ObjectMapper objectMapper;

    public SubscriptionEventListener(SendNotificationUseCase sendNotificationUseCase,
                                      ObjectMapper objectMapper) {
        this.sendNotificationUseCase = sendNotificationUseCase;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "subscription.activated.v1", groupId = "notification-sender")
    public void onSubscriptionActivated(@Payload String payload) {
        handle(payload, "subscription.activated.v1");
    }

    @KafkaListener(topics = "subscription.cancelled.v1", groupId = "notification-sender")
    public void onSubscriptionCancelled(@Payload String payload) {
        handle(payload, "subscription.cancelled.v1");
    }

    @KafkaListener(topics = "subscription.failed.v1", groupId = "notification-sender")
    public void onSubscriptionFailed(@Payload String payload) {
        handle(payload, "subscription.failed.v1");
    }

    @KafkaListener(topics = "subscription.renewed.v1", groupId = "notification-sender")
    public void onSubscriptionRenewed(@Payload String payload) {
        handle(payload, "subscription.renewed.v1");
    }

    @KafkaListener(topics = "subscription.suspended.v1", groupId = "notification-sender")
    public void onSubscriptionSuspended(@Payload String payload) {
        handle(payload, "subscription.suspended.v1");
    }

    private void handle(String payload, String topic) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            UUID userId = UUID.fromString(node.get("userId").asText());
            NotificationTemplate template = NotificationTemplate.forTopic(topic, node);
            log.info("Notification triggered: topic={} userId={}", topic, userId);
            sendNotificationUseCase.execute(new SendNotificationCommand(
                    userId, template.channel(), template.subject(), template.body()));
        } catch (Exception e) {
            log.error("Error processing notification for topic={}: {}", topic, e.getMessage(), e);
        }
    }
}
