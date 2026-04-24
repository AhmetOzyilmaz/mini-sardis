package com.mini.sardis.payment.application.port.out;

import java.math.BigDecimal;

public interface ExternalPaymentPort {
    ExternalPaymentResult charge(String idempotencyKey, BigDecimal amount, String currency);

    record ExternalPaymentResult(boolean success, String externalRef, String failureReason) {}
}
