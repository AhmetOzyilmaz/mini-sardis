package com.mini.sardis.infrastructure.adapter.in.web.dto;

import com.mini.sardis.application.port.in.subscription.SubscriptionResult;
import com.mini.sardis.domain.value.SubscriptionStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record SubscriptionResponse(
        UUID id,
        UUID userId,
        UUID planId,
        SubscriptionStatus status,
        LocalDate startDate,
        LocalDate nextRenewalDate,
        LocalDateTime createdAt,
        String message
) {
    public static SubscriptionResponse from(SubscriptionResult r) {
        String message = r.status() == SubscriptionStatus.PENDING
                ? "Aboneliğiniz oluşturuluyor / Your subscription is being created"
                : null;
        return new SubscriptionResponse(r.id(), r.userId(), r.planId(), r.status(),
                r.startDate(), r.nextRenewalDate(), r.createdAt(), message);
    }
}
