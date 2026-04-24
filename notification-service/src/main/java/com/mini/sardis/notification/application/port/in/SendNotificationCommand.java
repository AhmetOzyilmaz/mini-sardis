package com.mini.sardis.notification.application.port.in;

import com.mini.sardis.notification.domain.value.NotificationChannel;

import java.util.UUID;

public record SendNotificationCommand(
        UUID userId,
        NotificationChannel channel,
        String subject,
        String body
) {}
