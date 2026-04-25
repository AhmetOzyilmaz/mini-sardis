package com.mini.sardis.infrastructure.adapter.out.jpa;

import com.mini.sardis.application.port.out.PromoCodeRepositoryPort;
import com.mini.sardis.domain.entity.PromoCode;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class PromoCodeJpaAdapter implements PromoCodeRepositoryPort {

    private final JpaPromoCodeRepository jpaRepo;

    public PromoCodeJpaAdapter(JpaPromoCodeRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public Optional<PromoCode> findByCode(String code) {
        return jpaRepo.findByCode(code).map(this::toDomain);
    }

    @Override
    public PromoCode save(PromoCode promoCode) {
        return toDomain(jpaRepo.save(toJpa(promoCode)));
    }

    private PromoCodeJpaEntity toJpa(PromoCode p) {
        return PromoCodeJpaEntity.builder()
                .id(p.getId())
                .code(p.getCode())
                .discountType(p.getDiscountType())
                .discountValue(p.getDiscountValue())
                .maxUses(p.getMaxUses())
                .currentUses(p.getCurrentUses())
                .active(p.isActive())
                .validFrom(p.getValidFrom())
                .validTo(p.getValidTo())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private PromoCode toDomain(PromoCodeJpaEntity e) {
        return PromoCode.builder()
                .id(e.getId())
                .code(e.getCode())
                .discountType(e.getDiscountType())
                .discountValue(e.getDiscountValue())
                .maxUses(e.getMaxUses())
                .currentUses(e.getCurrentUses())
                .active(e.isActive())
                .validFrom(e.getValidFrom())
                .validTo(e.getValidTo())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
