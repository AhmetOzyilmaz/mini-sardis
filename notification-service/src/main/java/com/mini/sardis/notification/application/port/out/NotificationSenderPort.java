package com.mini.sardis.notification.application.port.out;

import com.mini.sardis.notification.domain.value.NotificationChannel;

public interface NotificationSenderPort {
    NotificationChannel channel();
    boolean send(String recipient, String subject, String body);
}
