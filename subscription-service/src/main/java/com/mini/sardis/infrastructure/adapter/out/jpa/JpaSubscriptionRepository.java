package com.mini.sardis.infrastructure.adapter.out.jpa;

import com.mini.sardis.domain.value.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface JpaSubscriptionRepository extends JpaRepository<SubscriptionJpaEntity, UUID> {
    List<SubscriptionJpaEntity> findByUserId(UUID userId);

    @Query("SELECT s FROM SubscriptionJpaEntity s WHERE s.status = :status AND s.nextRenewalDate <= :asOf")
    List<SubscriptionJpaEntity> findDueForRenewal(
            @Param("status") SubscriptionStatus status,
            @Param("asOf") LocalDate asOf);

    @Query("SELECT s FROM SubscriptionJpaEntity s WHERE s.status = :status AND s.gracePeriodEndDate < :asOf")
    List<SubscriptionJpaEntity> findExpiredGracePeriods(
            @Param("status") SubscriptionStatus status,
            @Param("asOf") LocalDate asOf);

    @Query("SELECT s FROM SubscriptionJpaEntity s WHERE s.cancelAtPeriodEnd = true AND s.nextRenewalDate <= :asOf AND s.status <> 'CANCELLED'")
    List<SubscriptionJpaEntity> findPendingCancelAtPeriodEnd(@Param("asOf") LocalDate asOf);
}
