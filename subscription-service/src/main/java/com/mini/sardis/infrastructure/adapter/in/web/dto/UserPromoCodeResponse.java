package com.mini.sardis.infrastructure.adapter.in.web.dto;

import com.mini.sardis.application.port.in.promo.UserPromoCodeResult;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserPromoCodeResponse(
        UUID id,
        UUID userId,
        String code,
        LocalDateTime assignedAt,
        boolean used,
        LocalDateTime usedAt
) {
    public static UserPromoCodeResponse from(UserPromoCodeResult r) {
        return new UserPromoCodeResponse(r.id(), r.userId(), r.code(),
                r.assignedAt(), r.used(), r.usedAt());
    }
}
