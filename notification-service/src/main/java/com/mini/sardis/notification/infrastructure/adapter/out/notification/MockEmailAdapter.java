package com.mini.sardis.notification.infrastructure.adapter.out.notification;

import com.mini.sardis.notification.application.port.out.NotificationSenderPort;
import com.mini.sardis.notification.domain.value.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simulates an email provider without any external service dependency.
 * Formats and prints a properly structured email to the application log.
 */
@Slf4j
@Component
public class MockEmailAdapter implements NotificationSenderPort {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss");
    private static final AtomicLong SENT_COUNT = new AtomicLong(0);

    @Override
    public NotificationChannel channel() { return NotificationChannel.EMAIL; }

    @Override
    public boolean send(String recipient, String subject, String body) {
        long seq = SENT_COUNT.incrementAndGet();
        String timestamp = LocalDateTime.now().format(FORMATTER);

        log.info("\n"
                + "╔══════════════════════════════════════════════════════════════╗\n"
                + "║  SIMULATED EMAIL  #{}\n"
                + "╠══════════════════════════════════════════════════════════════╣\n"
                + "║  From   : noreply@mini-sardis.com\n"
                + "║  To     : {} \n"
                + "║  Date   : {}\n"
                + "║  Subject: {}\n"
                + "╠══════════════════════════════════════════════════════════════╣\n"
                + "║\n"
                + "║  {}\n"
                + "║\n"
                + "╚══════════════════════════════════════════════════════════════╝",
                seq, recipient, timestamp, subject, body);

        return true;
    }
}
