package com.mini.sardis.payment.application.port.out;

import com.mini.sardis.payment.domain.value.PaymentMethod;

import java.math.BigDecimal;

public interface ExternalPaymentPort {
    ExternalPaymentResult charge(String idempotencyKey, BigDecimal amount, String currency,
                                  PaymentMethod paymentMethod);

    record ExternalPaymentResult(boolean success, String externalRef, String failureReason) {}
}
