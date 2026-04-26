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
import com.mini.sardis.application.port.out.UserPromoCodeRepositoryPort;
import com.mini.sardis.domain.entity.OutboxEvent;
import com.mini.sardis.domain.entity.PromoCode;
import com.mini.sardis.domain.entity.Subscription;
import com.mini.sardis.domain.entity.SubscriptionPlan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CreateSubscriptionService implements CreateSubscriptionUseCase {

    private final SubscriptionRepositoryPort subscriptionRepo;
    private final SubscriptionPlanRepositoryPort planRepo;
    private final OutboxRepositoryPort outboxRepo;
    private final ObjectMapper objectMapper;
    private final PromoCodeRepositoryPort promoCodeRepo;
    private final UserPromoCodeRepositoryPort userPromoCodeRepo;

    @Override
    @Transactional
    public SubscriptionResult execute(CreateSubscriptionCommand command) {
        SubscriptionPlan plan = planRepo.findById(command.planId())
                .filter(SubscriptionPlan::isActive)
                .orElseThrow(() -> new PlanNotFoundException(command.planId()));

        BigDecimal price = plan.getPrice().getAmount();
        String currency = plan.getPrice().getCurrency();

        Subscription subscription;
        int durationMonths = Math.max(1, (int) Math.round(plan.getDurationDays() / 30.0));

        if (command.promoCode() != null && !command.promoCode().isBlank()) {
            PromoCode promo = promoCodeRepo.findByCode(command.promoCode().toUpperCase())
                    .orElseThrow(() -> new InvalidPromoCodeException(
                            "Promo code '" + command.promoCode() + "' not found"));
            promo.validate(durationMonths);
            BigDecimal discount = promo.calculateDiscountAmount(price);
            promo.incrementUse();
            promoCodeRepo.save(promo);
            userPromoCodeRepo.findByUserIdAndCode(command.userId(), promo.getCode())
                    .ifPresent(upc -> {
                        upc.markUsed();
                        userPromoCodeRepo.save(upc);
                    });
            subscription = Subscription.createWithPromo(
                    command.userId(), plan.getId(), price, currency, promo.getId(), discount);
        } else {
            subscription = Subscription.create(command.userId(), plan.getId(), price, currency);
        }

        Subscription saved = subscriptionRepo.save(subscription);

        OutboxEvent event = OutboxEvent.create(
                saved.getId(), "Subscription", "subscription.created.v1",
                buildPayload(saved, plan, command));
        outboxRepo.save(event);

        return toResult(saved);
    }

    private String buildPayload(Subscription s, SubscriptionPlan plan, CreateSubscriptionCommand command) {
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
            payload.put("durationMonths", Math.max(1, (int) Math.round(plan.getDurationDays() / 30.0)));
            payload.put("idempotencyKey", s.getId().toString());
            if (command.paymentMethod() != null && !command.paymentMethod().isBlank()) {
                payload.put("paymentMethod", command.paymentMethod());
            }
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
