package com.mini.sardis.infrastructure.adapter.out.jpa;

import com.mini.sardis.application.port.out.UserPromoCodeRepositoryPort;
import com.mini.sardis.domain.entity.UserPromoCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JpaUserPromoCodeRepositoryAdapter implements UserPromoCodeRepositoryPort {

    private final JpaUserPromoCodeRepository jpaRepo;

    @Override
    public UserPromoCode save(UserPromoCode upc) {
        return toDomain(jpaRepo.save(toJpa(upc)));
    }

    @Override
    public List<UserPromoCode> saveAll(List<UserPromoCode> assignments) {
        return jpaRepo.saveAll(assignments.stream().map(this::toJpa).toList())
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<UserPromoCode> findByUserId(UUID userId) {
        return jpaRepo.findByUserId(userId).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<UserPromoCode> findByUserIdAndPromoCodeId(UUID userId, UUID promoCodeId) {
        return jpaRepo.findByUserIdAndPromoCodeId(userId, promoCodeId).map(this::toDomain);
    }

    @Override
    public Optional<UserPromoCode> findByUserIdAndCode(UUID userId, String code) {
        return jpaRepo.findByUserIdAndCode(userId, code).map(this::toDomain);
    }

    private UserPromoCodeJpaEntity toJpa(UserPromoCode upc) {
        return UserPromoCodeJpaEntity.builder()
                .id(upc.getId())
                .userId(upc.getUserId())
                .promoCodeId(upc.getPromoCodeId())
                .code(upc.getCode())
                .assignedAt(upc.getAssignedAt())
                .used(upc.isUsed())
                .usedAt(upc.getUsedAt())
                .build();
    }

    private UserPromoCode toDomain(UserPromoCodeJpaEntity e) {
        return UserPromoCode.reconstruct(
                e.getId(), e.getUserId(), e.getPromoCodeId(), e.getCode(),
                e.getAssignedAt(), e.isUsed(), e.getUsedAt());
    }
}
