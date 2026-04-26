package com.mini.sardis.application.service.subscription;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mini.sardis.application.exception.SubscriptionNotFoundException;
import com.mini.sardis.application.port.in.subscription.ReactivateSubscriptionUseCase;
import com.mini.sardis.application.port.in.subscription.SubscriptionResult;
import com.mini.sardis.application.port.out.OutboxRepositoryPort;
import com.mini.sardis.application.port.out.SubscriptionRepositoryPort;
import com.mini.sardis.domain.entity.OutboxEvent;
import com.mini.sardis.domain.entity.Subscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReactivateSubscriptionService implements ReactivateSubscriptionUseCase {

    private final SubscriptionRepositoryPort subscriptionRepo;
    private final OutboxRepositoryPort outboxRepo;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public SubscriptionResult execute(UUID subscriptionId, UUID requestingUserId) {
        Subscription subscription = subscriptionRepo.findById(subscriptionId)
                .orElseThrow(() -> new SubscriptionNotFoundException(subscriptionId));

        if (!subscription.getUserId().equals(requestingUserId)) {
            throw new AccessDeniedException("Cannot reactivate another user's subscription");
        }

        subscription.reactivate();
        Subscription saved = subscriptionRepo.save(subscription);

        outboxRepo.save(OutboxEvent.create(subscriptionId, "Subscription",
                "subscription.activated.v1", buildPayload(saved)));

        log.info("Subscription reactivated: {}", subscriptionId);
        return toResult(saved);
    }

    private String buildPayload(Subscription s) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "subscriptionId", s.getId().toString(),
                    "userId", s.getUserId().toString(),
                    "nextRenewalDate", s.getNextRenewalDate() != null ? s.getNextRenewalDate().toString() : "",
                    "paymentType", "REACTIVATE"
            ));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize outbox payload", e);
        }
    }

    private SubscriptionResult toResult(Subscription s) {
        return new SubscriptionResult(s.getId(), s.getUserId(), s.getPlanId(),
                s.getStatus(), s.getStartDate(), s.getNextRenewalDate(), s.getCreatedAt(),
                s.getAmount(), s.getDiscountAmount(), s.getFinalAmount());
    }
}
