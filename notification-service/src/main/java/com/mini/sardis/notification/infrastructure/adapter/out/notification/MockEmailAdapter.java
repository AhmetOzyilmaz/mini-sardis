package com.mini.sardis.notification.infrastructure.adapter.out.notification;

import com.mini.sardis.notification.application.port.out.NotificationSenderPort;
import com.mini.sardis.notification.domain.value.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MockEmailAdapter implements NotificationSenderPort {

    private static final Logger log = LoggerFactory.getLogger(MockEmailAdapter.class);

    @Override
    public NotificationChannel channel() { return NotificationChannel.EMAIL; }

    @Override
    public boolean send(String recipient, String subject, String body) {
        log.info("MOCK EMAIL ─────────────────────────────────────────────");
        log.info("  To     : {}", recipient);
        log.info("  Subject: {}", subject);
        log.info("  Body   : {}", body);
        log.info("────────────────────────────────────────────────────────");
        return true;
    }
}
