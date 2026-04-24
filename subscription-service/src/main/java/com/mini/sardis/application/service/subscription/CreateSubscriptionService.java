package com.mini.sardis.application.service.subscription;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mini.sardis.application.exception.PlanNotFoundException;
import com.mini.sardis.application.port.in.subscription.CreateSubscriptionCommand;
import com.mini.sardis.application.port.in.subscription.CreateSubscriptionUseCase;
import com.mini.sardis.application.port.in.subscription.SubscriptionResult;
import com.mini.sardis.application.port.out.OutboxRepositoryPort;
import com.mini.sardis.application.port.out.SubscriptionPlanRepositoryPort;
import com.mini.sardis.application.port.out.SubscriptionRepositoryPort;
import com.mini.sardis.domain.entity.OutboxEvent;
import com.mini.sardis.domain.entity.Subscription;
import com.mini.sardis.domain.entity.SubscriptionPlan;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class CreateSubscriptionService implements CreateSubscriptionUseCase {

    private final SubscriptionRepositoryPort subscriptionRepo;
    private final SubscriptionPlanRepositoryPort planRepo;
    private final OutboxRepositoryPort outboxRepo;
    private final ObjectMapper objectMapper;

    public CreateSubscriptionService(SubscriptionRepositoryPort subscriptionRepo,
                                     SubscriptionPlanRepositoryPort planRepo,
                                     OutboxRepositoryPort outboxRepo,
                                     ObjectMapper objectMapper) {
        this.subscriptionRepo = subscriptionRepo;
        this.planRepo = planRepo;
        this.outboxRepo = outboxRepo;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public SubscriptionResult execute(CreateSubscriptionCommand command) {
        SubscriptionPlan plan = planRepo.findById(command.planId())
                .filter(SubscriptionPlan::isActive)
                .orElseThrow(() -> new PlanNotFoundException(command.planId()));

        Subscription subscription = Subscription.create(
                command.userId(), plan.getId(),
                plan.getPrice().getAmount(), plan.getPrice().getCurrency());

        Subscription saved = subscriptionRepo.save(subscription);

        OutboxEvent event = OutboxEvent.create(
                saved.getId(), "Subscription", "subscription.created.v1",
                buildPayload(saved, plan));
        outboxRepo.save(event);

        return toResult(saved);
    }

    private String buildPayload(Subscription s, SubscriptionPlan plan) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "subscriptionId", s.getId().toString(),
                    "userId", s.getUserId().toString(),
                    "planId", s.getPlanId().toString(),
                    "planName", plan.getName(),
                    "amount", s.getAmount(),
                    "currency", s.getCurrency(),
                    "durationDays", plan.getDurationDays(),
                    "idempotencyKey", s.getId().toString()
            ));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize outbox payload", e);
        }
    }

    private SubscriptionResult toResult(Subscription s) {
        return new SubscriptionResult(s.getId(), s.getUserId(), s.getPlanId(),
                s.getStatus(), s.getStartDate(), s.getNextRenewalDate(), s.getCreatedAt());
    }
}
