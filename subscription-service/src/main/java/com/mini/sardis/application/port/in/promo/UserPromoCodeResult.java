package com.mini.sardis.application.port.in.promo;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserPromoCodeResult(
        UUID id,
        UUID userId,
        String code,
        LocalDateTime assignedAt,
        boolean used,
        LocalDateTime usedAt
) {}
