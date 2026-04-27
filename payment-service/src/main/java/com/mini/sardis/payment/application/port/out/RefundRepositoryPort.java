package com.mini.sardis.payment.application.port.out;

import com.mini.sardis.payment.domain.entity.Refund;

import java.util.Optional;
import java.util.UUID;

public interface RefundRepositoryPort {
    Refund save(Refund refund);
    Optional<Refund> findBySubscriptionId(UUID subscriptionId);
}
