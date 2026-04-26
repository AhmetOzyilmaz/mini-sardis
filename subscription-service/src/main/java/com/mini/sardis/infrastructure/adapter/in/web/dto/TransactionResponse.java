package com.mini.sardis.infrastructure.adapter.in.web.dto;

import com.mini.sardis.application.port.in.transaction.TransactionRecord;

import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionResponse(
        UUID eventId,
        UUID subscriptionId,
        String eventType,
        String payloadSummary,
        LocalDateTime occurredAt
) {
    public static TransactionResponse from(TransactionRecord r) {
        return new TransactionResponse(r.eventId(), r.subscriptionId(), r.eventType(),
                r.payloadSummary(), r.occurredAt());
    }
}
