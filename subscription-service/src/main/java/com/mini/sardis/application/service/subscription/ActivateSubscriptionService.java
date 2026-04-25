package com.mini.sardis.application.service.subscription;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mini.sardis.application.exception.SubscriptionNotFoundException;
import com.mini.sardis.application.port.in.subscription.ActivateSubscriptionUseCase;
import com.mini.sardis.application.port.out.OutboxRepositoryPort;
import com.mini.sardis.application.port.out.SubscriptionPlanRepositoryPort;
import com.mini.sardis.application.port.out.SubscriptionRepositoryPort;
import com.mini.sardis.domain.entity.OutboxEvent;
import com.mini.sardis.domain.entity.Subscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivateSubscriptionService implements ActivateSubscriptionUseCase {

    private final SubscriptionRepositoryPort subscriptionRepo;
    private final SubscriptionPlanRepositoryPort planRepo;
    private final OutboxRepositoryPort outboxRepo;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void execute(UUID subscriptionId, String paymentType) {
        Subscription subscription = subscriptionRepo.findById(subscriptionId)
                .orElseThrow(() -> new SubscriptionNotFoundException(subscriptionId));

        int durationDays = planRepo.findById(subscription.getPlanId())
                .map(p -> p.getDurationDays())
                .orElse(30);

        if ("RENEWAL".equals(paymentType)) {
            subscription.extendRenewal(durationDays);
            subscriptionRepo.save(subscription);
            outboxRepo.save(OutboxEvent.create(subscriptionId, "Subscription",
                    "subscription.renewed.v1", buildPayload(subscription, paymentType)));
            log.info("Subscription renewed: {}", subscriptionId);
        } else {
            subscription.activate(durationDays);
            subscriptionRepo.save(subscription);
            outboxRepo.save(OutboxEvent.create(subscriptionId, "Subscription",
                    "subscription.activated.v1", buildPayload(subscription, paymentType)));
            log.info("Subscription activated: {}", subscriptionId);
        }
    }

    private String buildPayload(Subscription s, String paymentType) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "subscriptionId", s.getId().toString(),
                    "userId", s.getUserId().toString(),
                    "nextRenewalDate", s.getNextRenewalDate() != null ? s.getNextRenewalDate().toString() : "",
                    "paymentType", paymentType
            ));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize outbox payload", e);
        }
    }
}
