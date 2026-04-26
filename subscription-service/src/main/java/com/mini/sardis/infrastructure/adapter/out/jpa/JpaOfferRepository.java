package com.mini.sardis.infrastructure.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface JpaOfferRepository extends JpaRepository<OfferJpaEntity, UUID> {

    @Query("SELECT o FROM OfferJpaEntity o WHERE o.active = true AND o.targetType = 'ALL_USERS'")
    List<OfferJpaEntity> findAllActive();

    @Query("SELECT o FROM OfferJpaEntity o WHERE o.active = true AND o.targetUserId = :userId")
    List<OfferJpaEntity> findActiveByTargetUserId(@Param("userId") UUID userId);
}
