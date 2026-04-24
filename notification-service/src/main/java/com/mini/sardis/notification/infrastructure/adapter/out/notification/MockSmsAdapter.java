package com.mini.sardis.notification.infrastructure.adapter.out.notification;

import com.mini.sardis.notification.application.port.out.NotificationSenderPort;
import com.mini.sardis.notification.domain.value.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MockSmsAdapter implements NotificationSenderPort {

    private static final Logger log = LoggerFactory.getLogger(MockSmsAdapter.class);

    @Override
    public NotificationChannel channel() { return NotificationChannel.SMS; }

    @Override
    public boolean send(String recipient, String subject, String body) {
        log.info("MOCK SMS ────────────────────────────────────────────────");
        log.info("  To  : {}", recipient);
        log.info("  Text: {}", body);
        log.info("────────────────────────────────────────────────────────");
        return true;
    }
}
