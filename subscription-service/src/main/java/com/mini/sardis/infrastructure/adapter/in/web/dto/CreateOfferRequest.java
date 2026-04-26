package com.mini.sardis.infrastructure.adapter.in.web.dto;

import com.mini.sardis.domain.value.OfferTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateOfferRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description,
        @NotNull UUID planId,
        UUID promoCodeId,
        @NotNull OfferTargetType targetType,
        UUID targetUserId,
        UUID targetPlanId,
        LocalDateTime validFrom,
        LocalDateTime validTo
) {}
