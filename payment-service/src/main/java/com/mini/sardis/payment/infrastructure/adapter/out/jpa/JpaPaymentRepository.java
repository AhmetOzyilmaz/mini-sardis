package com.mini.sardis.payment.infrastructure.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaPaymentRepository extends JpaRepository<PaymentJpaEntity, UUID> {
    Optional<PaymentJpaEntity> findByIdempotencyKey(String idempotencyKey);
    List<PaymentJpaEntity> findBySubscriptionId(UUID subscriptionId);

    @Query("SELECT p FROM PaymentJpaEntity p WHERE p.subscriptionId = :subscriptionId AND p.status = 'SUCCESS' ORDER BY p.processedAt DESC LIMIT 1")
    Optional<PaymentJpaEntity> findLatestSuccessBySubscriptionId(@Param("subscriptionId") UUID subscriptionId);
}
