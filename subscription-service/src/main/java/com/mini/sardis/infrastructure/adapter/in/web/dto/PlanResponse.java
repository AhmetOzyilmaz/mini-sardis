package com.mini.sardis.infrastructure.adapter.in.web.dto;

import com.mini.sardis.application.port.in.plan.PlanResult;

import java.math.BigDecimal;
import java.util.UUID;

public record PlanResponse(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        String currency,
        int durationDays,
        int trialDays
) {
    public static PlanResponse from(PlanResult r) {
        return new PlanResponse(r.id(), r.name(), r.description(),
                r.price(), r.currency(), r.durationDays(), r.trialDays());
    }
}
