package com.mini.sardis.application.service.subscription;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mini.sardis.application.exception.InvalidPromoCodeException;
import com.mini.sardis.application.exception.PlanNotFoundException;
import com.mini.sardis.application.port.in.subscription.CreateSubscriptionCommand;
import com.mini.sardis.application.port.in.subscription.CreateSubscriptionUseCase;
import com.mini.sardis.application.port.in.subscription.SubscriptionResult;
import com.mini.sardis.application.port.out.OutboxRepositoryPort;
import com.mini.sardis.application.port.out.PromoCodeRepositoryPort;
import com.mini.sardis.application.port.out.SubscriptionPlanRepositoryPort;
import com.mini.sardis.application.port.out.SubscriptionRepositoryPort;
import com.mini.sardis.domain.entity.OutboxEvent;
import com.mini.sardis.domain.entity.PromoCode;
import com.mini.sardis.domain.entity.Subscription;
import com.mini.sardis.domain.entity.SubscriptionPlan;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class CreateSubscriptionService implements CreateSubscriptionUseCase {

    private final SubscriptionRepositoryPort subscriptionRepo;
    private final SubscriptionPlanRepositoryPort planRepo;
    private final OutboxRepositoryPort outboxRepo;
    private final ObjectMapper objectMapper;
    private final PromoCodeRepositoryPort promoCodeRepo;

    public CreateSubscriptionService(SubscriptionRepositoryPort subscriptionRepo,
                                     SubscriptionPlanRepositoryPort planRepo,
                                     OutboxRepositoryPort outboxRepo,
                                     ObjectMapper objectMapper,
                                     PromoCodeRepositoryPort promoCodeRepo) {
        this.subscriptionRepo = subscriptionRepo;
        this.planRepo = planRepo;
        this.outboxRepo = outboxRepo;
        this.objectMapper = objectMapper;
        this.promoCodeRepo = promoCodeRepo;
    }

    @Override
    @Transactional
    public SubscriptionResult execute(CreateSubscriptionCommand command) {
        SubscriptionPlan plan = planRepo.findById(command.planId())
                .filter(SubscriptionPlan::isActive)
                .orElseThrow(() -> new PlanNotFoundException(command.planId()));

        BigDecimal price = plan.getPrice().getAmount();
        String currency = plan.getPrice().getCurrency();

        Subscription subscription;
        if (command.promoCode() != null && !command.promoCode().isBlank()) {
            PromoCode promo = promoCodeRepo.findByCode(command.promoCode().toUpperCase())
                    .orElseThrow(() -> new InvalidPromoCodeException(
                            "Promo code '" + command.promoCode() + "' not found"));
            promo.validate();
            BigDecimal discount = promo.calculateDiscountAmount(price);
            promo.incrementUse();
            promoCodeRepo.save(promo);
            subscription = Subscription.createWithPromo(
                    command.userId(), plan.getId(), price, currency, promo.getId(), discount);
        } else {
            subscription = Subscription.create(command.userId(), plan.getId(), price, currency);
        }

        Subscription saved = subscriptionRepo.save(subscription);

        OutboxEvent event = OutboxEvent.create(
                saved.getId(), "Subscription", "subscription.created.v1",
                buildPayload(saved, plan));
        outboxRepo.save(event);

        return toResult(saved);
    }

    private String buildPayload(Subscription s, SubscriptionPlan plan) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("subscriptionId", s.getId().toString());
            payload.put("userId", s.getUserId().toString());
            payload.put("planId", s.getPlanId().toString());
            payload.put("planName", plan.getName());
            payload.put("amount", s.getAmount());
            payload.put("currency", s.getCurrency());
            payload.put("discountAmount", s.getDiscountAmount());
            payload.put("finalAmount", s.getFinalAmount());
            payload.put("durationDays", plan.getDurationDays());
            payload.put("idempotencyKey", s.getId().toString());
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize outbox payload", e);
        }
    }

    private SubscriptionResult toResult(Subscription s) {
        return new SubscriptionResult(
                s.getId(), s.getUserId(), s.getPlanId(),
                s.getStatus(), s.getStartDate(), s.getNextRenewalDate(), s.getCreatedAt(),
                s.getAmount(), s.getDiscountAmount(), s.getFinalAmount());
    }
}
