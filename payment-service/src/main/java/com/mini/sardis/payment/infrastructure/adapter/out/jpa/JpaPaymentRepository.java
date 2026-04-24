package com.mini.sardis.payment.infrastructure.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaPaymentRepository extends JpaRepository<PaymentJpaEntity, UUID> {
    Optional<PaymentJpaEntity> findByIdempotencyKey(String idempotencyKey);
    List<PaymentJpaEntity> findBySubscriptionId(UUID subscriptionId);
}
