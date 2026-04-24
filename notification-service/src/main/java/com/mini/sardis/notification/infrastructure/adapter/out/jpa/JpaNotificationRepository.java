package com.mini.sardis.notification.infrastructure.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaNotificationRepository extends JpaRepository<NotificationLogJpaEntity, UUID> {
    List<NotificationLogJpaEntity> findByUserId(UUID userId);
}
