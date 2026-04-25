package com.mini.sardis.payment.infrastructure.adapter.in.web;

import com.mini.sardis.payment.domain.entity.Payment;
import com.mini.sardis.payment.domain.value.PaymentMethod;
import com.mini.sardis.payment.domain.value.PaymentStatus;
import com.mini.sardis.payment.domain.value.PaymentType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID subscriptionId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        PaymentType type,
        PaymentMethod paymentMethod,
        String externalRef,
        String failureReason,
        int retryCount,
        LocalDateTime createdAt
) {
    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(p.getId(), p.getSubscriptionId(), p.getAmount(),
                p.getCurrency(), p.getStatus(), p.getType(), p.getPaymentMethod(),
                p.getExternalRef(), p.getFailureReason(), p.getRetryCount(), p.getCreatedAt());
    }
}
