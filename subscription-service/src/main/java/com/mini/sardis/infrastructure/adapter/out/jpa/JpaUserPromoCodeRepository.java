package com.mini.sardis.infrastructure.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaUserPromoCodeRepository extends JpaRepository<UserPromoCodeJpaEntity, UUID> {
    List<UserPromoCodeJpaEntity> findByUserId(UUID userId);
    Optional<UserPromoCodeJpaEntity> findByUserIdAndPromoCodeId(UUID userId, UUID promoCodeId);
    Optional<UserPromoCodeJpaEntity> findByUserIdAndCode(UUID userId, String code);
}
