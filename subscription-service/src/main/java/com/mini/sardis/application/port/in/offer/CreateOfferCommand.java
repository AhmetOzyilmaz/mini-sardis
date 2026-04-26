package com.mini.sardis.application.port.in.offer;

import com.mini.sardis.domain.value.OfferTargetType;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateOfferCommand(
        String name,
        String description,
        UUID planId,
        UUID promoCodeId,
        OfferTargetType targetType,
        UUID targetUserId,
        UUID targetPlanId,
        LocalDateTime validFrom,
        LocalDateTime validTo
) {}
