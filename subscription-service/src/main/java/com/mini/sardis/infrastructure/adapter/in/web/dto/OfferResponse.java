package com.mini.sardis.infrastructure.adapter.in.web.dto;

import com.mini.sardis.application.port.in.offer.OfferResult;
import com.mini.sardis.domain.value.OfferTargetType;

import java.time.LocalDateTime;
import java.util.UUID;

public record OfferResponse(
        UUID id,
        String name,
        String description,
        UUID planId,
        UUID promoCodeId,
        OfferTargetType targetType,
        LocalDateTime validFrom,
        LocalDateTime validTo,
        boolean active,
        LocalDateTime createdAt
) {
    public static OfferResponse from(OfferResult r) {
        return new OfferResponse(r.id(), r.name(), r.description(), r.planId(),
                r.promoCodeId(), r.targetType(), r.validFrom(), r.validTo(),
                r.active(), r.createdAt());
    }
}
