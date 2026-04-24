package com.mini.sardis.domain.entity;

import java.time.LocalDateTime;
import java.util.UUID;

public class OutboxEvent {

    private final UUID id;
    private final UUID aggregateId;
    private final String aggregateType;
    private final String eventType;
    private final String payload;
    private boolean processed;
    private final LocalDateTime createdAt;
    private LocalDateTime processedAt;

    private OutboxEvent(UUID id, UUID aggregateId, String aggregateType,
                        String eventType, String payload, boolean processed,
                        LocalDateTime createdAt, LocalDateTime processedAt) {
        this.id = id;
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.eventType = eventType;
        this.payload = payload;
        this.processed = processed;
        this.createdAt = createdAt;
        this.processedAt = processedAt;
    }

    public static OutboxEvent create(UUID aggregateId, String aggregateType,
                                     String eventType, String payload) {
        return new OutboxEvent(UUID.randomUUID(), aggregateId, aggregateType,
                eventType, payload, false, LocalDateTime.now(), null);
    }

    public static OutboxEvent reconstruct(UUID id, UUID aggregateId, String aggregateType,
                                          String eventType, String payload, boolean processed,
                                          LocalDateTime createdAt, LocalDateTime processedAt) {
        return new OutboxEvent(id, aggregateId, aggregateType, eventType, payload,
                processed, createdAt, processedAt);
    }

    public void markProcessed() {
        this.processed = true;
        this.processedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getAggregateId() { return aggregateId; }
    public String getAggregateType() { return aggregateType; }
    public String getEventType() { return eventType; }
    public String getPayload() { return payload; }
    public boolean isProcessed() { return processed; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getProcessedAt() { return processedAt; }
}
