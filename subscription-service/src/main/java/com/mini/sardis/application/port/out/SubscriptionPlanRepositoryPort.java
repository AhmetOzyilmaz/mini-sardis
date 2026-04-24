package com.mini.sardis.application.port.out;

import com.mini.sardis.domain.entity.SubscriptionPlan;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionPlanRepositoryPort {
    List<SubscriptionPlan> findAllActive();
    Optional<SubscriptionPlan> findById(UUID id);
}
