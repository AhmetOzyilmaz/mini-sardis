package com.mini.sardis.infrastructure.adapter.out.jpa;

import com.mini.sardis.application.port.out.SubscriptionRepositoryPort;
import com.mini.sardis.domain.entity.Subscription;
import com.mini.sardis.domain.value.SubscriptionStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JpaSubscriptionRepositoryAdapter implements SubscriptionRepositoryPort {

    private final JpaSubscriptionRepository jpaRepo;

    public JpaSubscriptionRepositoryAdapter(JpaSubscriptionRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public Subscription save(Subscription subscription) {
        SubscriptionJpaEntity entity = toJpa(subscription);
        return toDomain(jpaRepo.save(entity));
    }

    @Override
    public Optional<Subscription> findById(UUID id) {
        return jpaRepo.findById(id).map(this::toDomain);
    }

    @Override
    public List<Subscription> findByUserId(UUID userId) {
        return jpaRepo.findByUserId(userId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Subscription> findAll() {
        return jpaRepo.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public List<Subscription> findDueForRenewal(LocalDate asOf) {
        return jpaRepo.findDueForRenewal(SubscriptionStatus.ACTIVE, asOf)
                .stream().map(this::toDomain).toList();
    }

    private SubscriptionJpaEntity toJpa(Subscription s) {
        return SubscriptionJpaEntity.builder()
                .id(s.getId())
                .userId(s.getUserId())
                .planId(s.getPlanId())
                .status(s.getStatus())
                .startDate(s.getStartDate())
                .endDate(s.getEndDate())
                .nextRenewalDate(s.getNextRenewalDate())
                .cancelledAt(s.getCancelledAt())
                .cancellationReason(s.getCancellationReason())
                .version(s.getVersion())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .amount(s.getAmount())
                .currency(s.getCurrency())
                .promoCodeId(s.getPromoCodeId())
                .discountAmount(s.getDiscountAmount())
                .finalAmount(s.getFinalAmount())
                .build();
    }

    private Subscription toDomain(SubscriptionJpaEntity e) {
        return Subscription.builder()
                .id(e.getId())
                .userId(e.getUserId())
                .planId(e.getPlanId())
                .status(e.getStatus())
                .startDate(e.getStartDate())
                .endDate(e.getEndDate())
                .nextRenewalDate(e.getNextRenewalDate())
                .cancelledAt(e.getCancelledAt())
                .cancellationReason(e.getCancellationReason())
                .version(e.getVersion())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .amount(e.getAmount())
                .currency(e.getCurrency())
                .promoCodeId(e.getPromoCodeId())
                .discountAmount(e.getDiscountAmount())
                .finalAmount(e.getFinalAmount())
                .build();
    }
}
