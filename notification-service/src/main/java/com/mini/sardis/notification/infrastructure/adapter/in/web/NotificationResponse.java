package com.mini.sardis.notification.infrastructure.adapter.in.web;

import com.mini.sardis.notification.domain.entity.NotificationLog;
import com.mini.sardis.notification.domain.value.NotificationChannel;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID userId,
        NotificationChannel channel,
        String subject,
        String body,
        boolean success,
        LocalDateTime sentAt
) {
    public static NotificationResponse from(NotificationLog n) {
        return new NotificationResponse(n.getId(), n.getUserId(), n.getChannel(),
                n.getSubject(), n.getBody(), n.isSuccess(), n.getSentAt());
    }
}
