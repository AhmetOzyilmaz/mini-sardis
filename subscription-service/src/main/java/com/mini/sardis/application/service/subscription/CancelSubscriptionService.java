package com.mini.sardis.application.service.subscription;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mini.sardis.application.exception.SubscriptionNotFoundException;
import com.mini.sardis.application.port.in.subscription.CancelSubscriptionUseCase;
import com.mini.sardis.application.port.out.OutboxRepositoryPort;
import com.mini.sardis.application.port.out.SubscriptionRepositoryPort;
import com.mini.sardis.domain.entity.OutboxEvent;
import com.mini.sardis.domain.entity.Subscription;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CancelSubscriptionService implements CancelSubscriptionUseCase {

    private final SubscriptionRepositoryPort subscriptionRepo;
    private final OutboxRepositoryPort outboxRepo;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void execute(UUID subscriptionId, UUID requestingUserId, String reason) {
        Subscription subscription = subscriptionRepo.findById(subscriptionId)
                .orElseThrow(() -> new SubscriptionNotFoundException(subscriptionId));

        if (!subscription.getUserId().equals(requestingUserId)) {
            throw new AccessDeniedException("Cannot cancel another user's subscription");
        }

        subscription.cancel(reason);
        subscriptionRepo.save(subscription);

        OutboxEvent event = OutboxEvent.create(
                subscription.getId(), "Subscription", "subscription.cancelled.v1",
                buildPayload(subscription, reason));
        outboxRepo.save(event);
    }

    private String buildPayload(Subscription s, String reason) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "subscriptionId", s.getId().toString(),
                    "userId", s.getUserId().toString(),
                    "reason", reason != null ? reason : "user_request"
            ));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize outbox payload", e);
        }
    }
}
