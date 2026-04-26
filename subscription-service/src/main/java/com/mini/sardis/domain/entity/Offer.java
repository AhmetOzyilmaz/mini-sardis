package com.mini.sardis.domain.entity;

import com.mini.sardis.domain.value.OfferTargetType;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class Offer {

    private final UUID id;
    private final String name;
    private final String description;
    private final UUID planId;
    private final UUID promoCodeId;
    private final OfferTargetType targetType;
    private final UUID targetUserId;
    private final UUID targetPlanId;
    private final LocalDateTime validFrom;
    private final LocalDateTime validTo;
    private boolean active;
    private final LocalDateTime createdAt;

    private Offer(UUID id, String name, String description, UUID planId, UUID promoCodeId,
                  OfferTargetType targetType, UUID targetUserId, UUID targetPlanId,
                  LocalDateTime validFrom, LocalDateTime validTo, boolean active, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.planId = planId;
        this.promoCodeId = promoCodeId;
        this.targetType = targetType;
        this.targetUserId = targetUserId;
        this.targetPlanId = targetPlanId;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.active = active;
        this.createdAt = createdAt;
    }

    public static Offer create(String name, String description, UUID planId, UUID promoCodeId,
                               OfferTargetType targetType, UUID targetUserId, UUID targetPlanId,
                               LocalDateTime validFrom, LocalDateTime validTo) {
        return new Offer(UUID.randomUUID(), name, description, planId, promoCodeId,
                targetType, targetUserId, targetPlanId, validFrom, validTo, true, LocalDateTime.now());
    }

    public static Offer reconstruct(UUID id, String name, String description, UUID planId, UUID promoCodeId,
                                    OfferTargetType targetType, UUID targetUserId, UUID targetPlanId,
                                    LocalDateTime validFrom, LocalDateTime validTo, boolean active, LocalDateTime createdAt) {
        return new Offer(id, name, description, planId, promoCodeId,
                targetType, targetUserId, targetPlanId, validFrom, validTo, active, createdAt);
    }

    public boolean isEligibleFor(UUID userId, UUID currentPlanId) {
        if (!active) return false;
        LocalDateTime now = LocalDateTime.now();
        if (validFrom != null && now.isBefore(validFrom)) return false;
        if (validTo != null && now.isAfter(validTo)) return false;
        return switch (targetType) {
            case ALL_USERS -> true;
            case SPECIFIC_USER -> userId != null && userId.equals(targetUserId);
            case PLAN_UPGRADE -> currentPlanId != null && currentPlanId.equals(targetPlanId);
        };
    }
}
