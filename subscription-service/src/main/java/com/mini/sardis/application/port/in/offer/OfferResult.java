package com.mini.sardis.application.port.in.offer;

import com.mini.sardis.domain.entity.Offer;
import com.mini.sardis.domain.value.OfferTargetType;

import java.time.LocalDateTime;
import java.util.UUID;

public record OfferResult(
        UUID id,
        String name,
        String description,
        UUID planId,
        UUID promoCodeId,
        OfferTargetType targetType,
        UUID targetUserId,
        UUID targetPlanId,
        LocalDateTime validFrom,
        LocalDateTime validTo,
        boolean active,
        LocalDateTime createdAt
) {
    public static OfferResult from(Offer o) {
        return new OfferResult(o.getId(), o.getName(), o.getDescription(), o.getPlanId(),
                o.getPromoCodeId(), o.getTargetType(), o.getTargetUserId(), o.getTargetPlanId(),
                o.getValidFrom(), o.getValidTo(), o.isActive(), o.getCreatedAt());
    }
}
