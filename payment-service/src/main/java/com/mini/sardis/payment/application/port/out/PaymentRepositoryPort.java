package com.mini.sardis.payment.application.port.out;

import com.mini.sardis.payment.domain.entity.Payment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepositoryPort {
    Payment save(Payment payment);
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    List<Payment> findBySubscriptionId(UUID subscriptionId);
}
