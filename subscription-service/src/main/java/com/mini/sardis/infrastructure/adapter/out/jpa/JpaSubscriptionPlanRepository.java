package com.mini.sardis.infrastructure.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaSubscriptionPlanRepository extends JpaRepository<SubscriptionPlanJpaEntity, UUID> {
    List<SubscriptionPlanJpaEntity> findByActiveTrue();
}
