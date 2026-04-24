package com.mini.sardis.notification.domain.entity;

import com.mini.sardis.notification.domain.value.NotificationChannel;

import java.time.LocalDateTime;
import java.util.UUID;

public class NotificationLog {

    private final UUID id;
    private final UUID userId;
    private final NotificationChannel channel;
    private final String subject;
    private final String body;
    private final boolean success;
    private final LocalDateTime sentAt;

    private NotificationLog(UUID id, UUID userId, NotificationChannel channel,
                            String subject, String body, boolean success, LocalDateTime sentAt) {
        this.id = id;
        this.userId = userId;
        this.channel = channel;
        this.subject = subject;
        this.body = body;
        this.success = success;
        this.sentAt = sentAt;
    }

    public static NotificationLog create(UUID userId, NotificationChannel channel,
                                         String subject, String body, boolean success) {
        return new NotificationLog(UUID.randomUUID(), userId, channel,
                subject, body, success, LocalDateTime.now());
    }

    public static NotificationLog reconstruct(UUID id, UUID userId, NotificationChannel channel,
                                               String subject, String body, boolean success,
                                               LocalDateTime sentAt) {
        return new NotificationLog(id, userId, channel, subject, body, success, sentAt);
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public NotificationChannel getChannel() { return channel; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public boolean isSuccess() { return success; }
    public LocalDateTime getSentAt() { return sentAt; }
}
