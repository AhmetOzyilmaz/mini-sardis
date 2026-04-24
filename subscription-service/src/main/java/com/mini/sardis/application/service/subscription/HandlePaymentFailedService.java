package com.mini.sardis.application.service.subscription;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mini.sardis.application.exception.SubscriptionNotFoundException;
import com.mini.sardis.application.port.in.subscription.HandlePaymentFailedUseCase;
import com.mini.sardis.application.port.out.OutboxRepositoryPort;
import com.mini.sardis.application.port.out.SubscriptionRepositoryPort;
import com.mini.sardis.domain.entity.OutboxEvent;
import com.mini.sardis.domain.entity.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class HandlePaymentFailedService implements HandlePaymentFailedUseCase {

    private static final Logger log = LoggerFactory.getLogger(HandlePaymentFailedService.class);

    private final SubscriptionRepositoryPort subscriptionRepo;
    private final OutboxRepositoryPort outboxRepo;
    private final ObjectMapper objectMapper;

    public HandlePaymentFailedService(SubscriptionRepositoryPort subscriptionRepo,
                                      OutboxRepositoryPort outboxRepo,
                                      ObjectMapper objectMapper) {
        this.subscriptionRepo = subscriptionRepo;
        this.outboxRepo = outboxRepo;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void execute(UUID subscriptionId, String paymentType, String reason) {
        Subscription subscription = subscriptionRepo.findById(subscriptionId)
                .orElseThrow(() -> new SubscriptionNotFoundException(subscriptionId));

        String eventType;
        if ("RENEWAL".equals(paymentType)) {
            subscription.suspend();
            eventType = "subscription.suspended.v1";
            log.info("Subscription suspended after renewal failure: {}", subscriptionId);
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
            return objectMapper.writeValueAsString(Map.of(
                    "subscriptionId", s.getId().toString(),
                    "userId", s.getUserId().toString(),
                    "reason", reason != null ? reason : "unknown",
                    "status", s.getStatus().name()
            ));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize outbox payload", e);
        }
    }
}
