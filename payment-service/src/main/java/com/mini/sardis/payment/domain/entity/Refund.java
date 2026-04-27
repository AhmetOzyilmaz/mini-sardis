package com.mini.sardis.payment.domain.entity;

import com.mini.sardis.payment.domain.value.RefundStatus;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class Refund {

    private final UUID id;
    private final UUID paymentId;
    private final UUID subscriptionId;
    private final UUID userId;
    private final BigDecimal amount;
    private final String currency;
    private RefundStatus status;
    private final String reason;
    private final LocalDateTime requestedAt;
    private LocalDateTime processedAt;

    private Refund(UUID id, UUID paymentId, UUID subscriptionId, UUID userId,
                   BigDecimal amount, String currency, RefundStatus status,
                   String reason, LocalDateTime requestedAt, LocalDateTime processedAt) {
        this.id = id;
        this.paymentId = paymentId;
        this.subscriptionId = subscriptionId;
        this.userId = userId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.reason = reason;
        this.requestedAt = requestedAt;
        this.processedAt = processedAt;
    }

    public static Refund create(UUID paymentId, UUID subscriptionId, UUID userId,
                                BigDecimal amount, String currency, String reason) {
        return new Refund(UUID.randomUUID(), paymentId, subscriptionId, userId,
                amount, currency, RefundStatus.PROCESSING, reason, LocalDateTime.now(), null);
    }

    public static Refund reconstruct(UUID id, UUID paymentId, UUID subscriptionId, UUID userId,
                                     BigDecimal amount, String currency, RefundStatus status,
                                     String reason, LocalDateTime requestedAt, LocalDateTime processedAt) {
        return new Refund(id, paymentId, subscriptionId, userId, amount, currency,
                status, reason, requestedAt, processedAt);
    }

    public void markCompleted() {
        this.status = RefundStatus.COMPLETED;
        this.processedAt = LocalDateTime.now();
    }

    public void markRejected(String rejectionReason) {
        this.status = RefundStatus.REJECTED;
        this.processedAt = LocalDateTime.now();
    }
}
