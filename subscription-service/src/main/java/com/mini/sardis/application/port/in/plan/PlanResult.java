package com.mini.sardis.application.port.in.plan;

import java.math.BigDecimal;
import java.util.UUID;

public record PlanResult(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        String currency,
        int durationDays,
        int trialDays,
        boolean active
) {}
