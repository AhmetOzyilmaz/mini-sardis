package com.mini.sardis.infrastructure.adapter.out.jpa;

import com.mini.sardis.application.port.out.OfferRepositoryPort;
import com.mini.sardis.domain.entity.Offer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JpaOfferRepositoryAdapter implements OfferRepositoryPort {

    private final JpaOfferRepository jpaRepo;

    @Override
    public Offer save(Offer offer) {
        return toDomain(jpaRepo.save(toJpa(offer)));
    }

    @Override
    public Optional<Offer> findById(UUID id) {
        return jpaRepo.findById(id).map(this::toDomain);
    }

    @Override
    public List<Offer> findAllActive() {
        return jpaRepo.findAllActive().stream().map(this::toDomain).toList();
    }

    @Override
    public List<Offer> findActiveByTargetUserId(UUID userId) {
        return jpaRepo.findActiveByTargetUserId(userId).stream().map(this::toDomain).toList();
    }

    private OfferJpaEntity toJpa(Offer o) {
        return OfferJpaEntity.builder()
                .id(o.getId())
                .name(o.getName())
                .description(o.getDescription())
                .planId(o.getPlanId())
                .promoCodeId(o.getPromoCodeId())
                .targetType(o.getTargetType())
                .targetUserId(o.getTargetUserId())
                .targetPlanId(o.getTargetPlanId())
                .validFrom(o.getValidFrom())
                .validTo(o.getValidTo())
                .active(o.isActive())
                .createdAt(o.getCreatedAt())
                .build();
    }

    private Offer toDomain(OfferJpaEntity e) {
        return Offer.reconstruct(e.getId(), e.getName(), e.getDescription(), e.getPlanId(),
                e.getPromoCodeId(), e.getTargetType(), e.getTargetUserId(), e.getTargetPlanId(),
                e.getValidFrom(), e.getValidTo(), e.isActive(), e.getCreatedAt());
    }
}
