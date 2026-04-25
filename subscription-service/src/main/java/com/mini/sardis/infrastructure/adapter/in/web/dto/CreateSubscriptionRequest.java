package com.mini.sardis.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateSubscriptionRequest(
        @NotNull UUID planId,
        @Size(min = 5, max = 20) String promoCode,
        String paymentMethod
) {}
