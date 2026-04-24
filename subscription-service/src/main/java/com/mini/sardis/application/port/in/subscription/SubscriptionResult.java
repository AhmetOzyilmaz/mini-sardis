package com.mini.sardis.application.port.in.subscription;

import com.mini.sardis.domain.value.SubscriptionStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record SubscriptionResult(
        UUID id,
        UUID userId,
        UUID planId,
        SubscriptionStatus status,
        LocalDate startDate,
        LocalDate nextRenewalDate,
        LocalDateTime createdAt
) {}
