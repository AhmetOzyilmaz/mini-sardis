package com.mini.sardis.infrastructure.adapter.out.jpa;

import com.mini.sardis.application.port.out.PromoCodeRepositoryPort;
import com.mini.sardis.domain.entity.PromoCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PromoCodeJpaAdapter implements PromoCodeRepositoryPort {

    private final JpaPromoCodeRepository jpaRepo;

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
                .applicableMonths(serializeMonths(p.getApplicableMonths()))
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
                .applicableMonths(deserializeMonths(e.getApplicableMonths()))
                .build();
    }

    private String serializeMonths(Set<Integer> months) {
        if (months == null || months.isEmpty()) return null;
        return months.stream().sorted().map(String::valueOf).collect(Collectors.joining(","));
    }

    private Set<Integer> deserializeMonths(String value) {
        if (value == null || value.isBlank()) return null;
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .collect(Collectors.toSet());
    }
}
