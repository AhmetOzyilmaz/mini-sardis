package com.mini.sardis.infrastructure.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JpaPromoCodeRepository extends JpaRepository<PromoCodeJpaEntity, UUID> {
    Optional<PromoCodeJpaEntity> findByCode(String code);
}
