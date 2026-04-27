package com.mini.sardis.payment.infrastructure.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JpaRefundRepository extends JpaRepository<RefundJpaEntity, UUID> {
    Optional<RefundJpaEntity> findBySubscriptionId(UUID subscriptionId);
}
