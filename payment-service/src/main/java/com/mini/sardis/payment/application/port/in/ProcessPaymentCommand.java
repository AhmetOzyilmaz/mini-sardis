package com.mini.sardis.payment.application.port.in;

import com.mini.sardis.payment.domain.value.PaymentMethod;
import com.mini.sardis.payment.domain.value.PaymentType;

import java.math.BigDecimal;
import java.util.UUID;

public record ProcessPaymentCommand(
        UUID subscriptionId,
        UUID userId,
        String idempotencyKey,
        BigDecimal amount,
        String currency,
        PaymentType paymentType,
        PaymentMethod paymentMethod
) {
    public ProcessPaymentCommand(UUID subscriptionId, UUID userId, String idempotencyKey,
                                  BigDecimal amount, String currency, PaymentType paymentType) {
        this(subscriptionId, userId, idempotencyKey, amount, currency, paymentType, PaymentMethod.CREDIT_CARD);
    }
}
