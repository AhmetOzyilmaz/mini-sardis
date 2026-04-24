package com.mini.sardis.infrastructure.adapter.out.jpa;

import com.mini.sardis.application.port.out.SubscriptionPlanRepositoryPort;
import com.mini.sardis.domain.entity.SubscriptionPlan;
import com.mini.sardis.domain.value.Money;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JpaSubscriptionPlanRepositoryAdapter implements SubscriptionPlanRepositoryPort {

    private final JpaSubscriptionPlanRepository jpaRepo;

    public JpaSubscriptionPlanRepositoryAdapter(JpaSubscriptionPlanRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public List<SubscriptionPlan> findAllActive() {
        return jpaRepo.findByActiveTrue().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<SubscriptionPlan> findById(UUID id) {
        return jpaRepo.findById(id).map(this::toDomain);
    }

    private SubscriptionPlan toDomain(SubscriptionPlanJpaEntity e) {
        return SubscriptionPlan.builder()
                .id(e.getId())
                .name(e.getName())
                .description(e.getDescription())
                .price(Money.of(e.getPrice(), e.getCurrency() != null ? e.getCurrency() : "TRY"))
                .durationDays(e.getDurationDays())
                .trialDays(e.getTrialDays())
                .active(e.isActive())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
