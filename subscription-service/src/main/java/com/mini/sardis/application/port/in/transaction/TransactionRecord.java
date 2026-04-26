package com.mini.sardis.application.port.in.transaction;

import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionRecord(
        UUID eventId,
        UUID subscriptionId,
        String eventType,
        String payloadSummary,
        LocalDateTime occurredAt
) {}
