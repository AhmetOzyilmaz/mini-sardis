package com.mini.sardis.application.service.subscription;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mini.sardis.application.exception.SubscriptionNotFoundException;
import com.mini.sardis.application.port.in.subscription.HandlePaymentFailedUseCase;
import com.mini.sardis.application.port.out.OutboxRepositoryPort;
import com.mini.sardis.application.port.out.SubscriptionRepositoryPort;
import com.mini.sardis.domain.entity.OutboxEvent;
import com.mini.sardis.domain.entity.Subscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class HandlePaymentFailedService implements HandlePaymentFailedUseCase {

    private final SubscriptionRepositoryPort subscriptionRepo;
    private final OutboxRepositoryPort outboxRepo;
    private final ObjectMapper objectMapper;

    @Value("${app.grace-period.days:3}")
    private int gracePeriodDays;

    @Override
    @Transactional
    public void execute(UUID subscriptionId, String paymentType, String reason) {
        Subscription subscription = subscriptionRepo.findById(subscriptionId)
                .orElseThrow(() -> new SubscriptionNotFoundException(subscriptionId));

        String eventType;
        if ("RENEWAL".equals(paymentType)) {
            subscription.enterGracePeriod(gracePeriodDays);
            eventType = "subscription.grace_period.v1";
            log.info("Subscription entered grace period after renewal failure: {}", subscriptionId);
        } else {
            subscription.cancel("payment_failed: " + reason);
            eventType = "subscription.failed.v1";
            log.info("Subscription cancelled after initial payment failure: {}", subscriptionId);
        }

        subscriptionRepo.save(subscription);
        outboxRepo.save(OutboxEvent.create(subscriptionId, "Subscription", eventType,
                buildPayload(subscription, reason)));
    }

    private String buildPayload(Subscription s, String reason) {
        try {
            var payload = new java.util.HashMap<String, Object>();
            payload.put("subscriptionId", s.getId().toString());
            payload.put("userId", s.getUserId().toString());
            payload.put("reason", reason != null ? reason : "unknown");
            payload.put("status", s.getStatus().name());
            if (s.getGracePeriodEndDate() != null) {
                payload.put("gracePeriodEndDate", s.getGracePeriodEndDate().toString());
            }
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize outbox payload", e);
        }
    }
}
