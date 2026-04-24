package com.mini.sardis.notification.infrastructure.adapter.out.jpa;

import com.mini.sardis.notification.application.port.out.NotificationRepositoryPort;
import com.mini.sardis.notification.domain.entity.NotificationLog;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class JpaNotificationRepositoryAdapter implements NotificationRepositoryPort {

    private final JpaNotificationRepository jpaRepo;

    public JpaNotificationRepositoryAdapter(JpaNotificationRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public void save(NotificationLog log) {
        jpaRepo.save(toJpa(log));
    }

    @Override
    public List<NotificationLog> findByUserId(UUID userId) {
        return jpaRepo.findByUserId(userId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<NotificationLog> findAll() {
        return jpaRepo.findAll().stream().map(this::toDomain).toList();
    }

    private NotificationLogJpaEntity toJpa(NotificationLog n) {
        return NotificationLogJpaEntity.builder()
                .id(n.getId())
                .userId(n.getUserId())
                .channel(n.getChannel())
                .subject(n.getSubject())
                .body(n.getBody())
                .success(n.isSuccess())
                .sentAt(n.getSentAt())
                .build();
    }

    private NotificationLog toDomain(NotificationLogJpaEntity e) {
        return NotificationLog.reconstruct(e.getId(), e.getUserId(), e.getChannel(),
                e.getSubject(), e.getBody(), e.isSuccess(), e.getSentAt());
    }
}
