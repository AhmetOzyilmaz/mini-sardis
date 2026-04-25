package com.mini.sardis.notification.infrastructure.adapter.out.notification;

import com.mini.sardis.notification.application.port.out.NotificationSenderPort;
import com.mini.sardis.notification.domain.value.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simulates an SMS gateway without any external service dependency.
 * Formats and prints a properly structured SMS record to the application log.
 */
@Slf4j
@Component
public class MockSmsAdapter implements NotificationSenderPort {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss");
    private static final AtomicLong SENT_COUNT = new AtomicLong(0);
    private static final int SMS_SEGMENT_LENGTH = 160;

    @Override
    public NotificationChannel channel() { return NotificationChannel.SMS; }

    @Override
    public boolean send(String recipient, String subject, String body) {
        long seq = SENT_COUNT.incrementAndGet();
        String timestamp = LocalDateTime.now().format(FORMATTER);
        int chars = body.length();
        int segments = (int) Math.ceil((double) chars / SMS_SEGMENT_LENGTH);

        log.info("\n"
                + "╔══════════════════════════════════════════════════╗\n"
                + "║  SIMULATED SMS  #{}\n"
                + "╠══════════════════════════════════════════════════╣\n"
                + "║  From    : SARDIS\n"
                + "║  To      : {}\n"
                + "║  Sent    : {}\n"
                + "║  Length  : {} chars ({} segment{})\n"
                + "╠══════════════════════════════════════════════════╣\n"
                + "║  {}\n"
                + "╚══════════════════════════════════════════════════╝",
                seq, recipient, timestamp,
                chars, segments, segments > 1 ? "s" : "",
                body);

        return true;
    }
}
