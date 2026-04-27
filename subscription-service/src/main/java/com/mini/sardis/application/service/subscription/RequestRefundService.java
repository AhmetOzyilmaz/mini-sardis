package com.mini.sardis.application.service.subscription;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mini.sardis.application.exception.SubscriptionNotFoundException;
import com.mini.sardis.application.port.in.subscription.RequestRefundCommand;
import com.mini.sardis.application.port.in.subscription.RequestRefundUseCase;
import com.mini.sardis.application.port.out.OutboxRepositoryPort;
import com.mini.sardis.application.port.out.SubscriptionRepositoryPort;
import com.mini.sardis.domain.entity.OutboxEvent;
import com.mini.sardis.domain.entity.Subscription;
import com.mini.sardis.domain.value.SubscriptionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RequestRefundService implements RequestRefundUseCase {

    private static final Set<SubscriptionStatus> REFUNDABLE = Set.of(
            SubscriptionStatus.ACTIVE, SubscriptionStatus.SUSPENDED, SubscriptionStatus.GRACE_PERIOD);

    private final SubscriptionRepositoryPort subscriptionRepo;
    private final OutboxRepositoryPort outboxRepo;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void execute(RequestRefundCommand command) {
        Subscription subscription = subscriptionRepo.findById(command.subscriptionId())
                .orElseThrow(() -> new SubscriptionNotFoundException(command.subscriptionId()));

        if (!subscription.getUserId().equals(command.requestingUserId())) {
            throw new IllegalStateException("Subscription does not belong to requesting user");
        }

        if (!REFUNDABLE.contains(subscription.getStatus())) {
            throw new IllegalStateException("Cannot refund subscription in status: " + subscription.getStatus());
        }

        OutboxEvent event = OutboxEvent.create(
                subscription.getId(), "Subscription", "refund.requested.v1",
                buildPayload(command));
        outboxRepo.save(event);
    }

    private String buildPayload(RequestRefundCommand command) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "subscriptionId", command.subscriptionId().toString(),
                    "userId", command.requestingUserId().toString(),
                    "reason", command.reason() != null ? command.reason() : "user_request",
                    "requestedAt", LocalDateTime.now().toString()
            ));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize refund.requested.v1", e);
        }
    }
}
