package com.mini.sardis.notification.application.port.out;

import com.mini.sardis.notification.domain.entity.NotificationLog;

import java.util.List;
import java.util.UUID;

public interface NotificationRepositoryPort {
    void save(NotificationLog log);
    List<NotificationLog> findByUserId(UUID userId);
    List<NotificationLog> findAll();
}
