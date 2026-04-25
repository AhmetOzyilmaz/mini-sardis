package com.mini.sardis.payment.domain.entity;

import com.mini.sardis.payment.domain.value.PaymentMethod;
import com.mini.sardis.payment.domain.value.PaymentStatus;
import com.mini.sardis.payment.domain.value.PaymentType;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class Payment {

    private final UUID id;
    private final UUID subscriptionId;
    private final UUID userId;
    private final String idempotencyKey;
    private final BigDecimal amount;
    private final String currency;
    private PaymentStatus status;
    private final PaymentType type;
    private final PaymentMethod paymentMethod;
    private String externalRef;
    private String failureReason;
    private int retryCount;
    private LocalDateTime processedAt;
    private final LocalDateTime createdAt;

    private Payment(Builder b) {
        this.id = b.id;
        this.subscriptionId = b.subscriptionId;
        this.userId = b.userId;
        this.idempotencyKey = b.idempotencyKey;
        this.amount = b.amount;
        this.currency = b.currency;
        this.status = b.status;
        this.type = b.type;
        this.paymentMethod = b.paymentMethod;
        this.externalRef = b.externalRef;
        this.failureReason = b.failureReason;
        this.retryCount = b.retryCount;
        this.processedAt = b.processedAt;
        this.createdAt = b.createdAt;
    }

    public static Payment create(UUID subscriptionId, UUID userId, String idempotencyKey,
                                  BigDecimal amount, String currency, PaymentType type,
                                  PaymentMethod paymentMethod) {
        return new Builder()
                .id(UUID.randomUUID())
                .subscriptionId(subscriptionId)
                .userId(userId)
                .idempotencyKey(idempotencyKey)
                .amount(amount)
                .currency(currency)
                .status(PaymentStatus.PENDING)
                .type(type)
                .paymentMethod(paymentMethod != null ? paymentMethod : PaymentMethod.CREDIT_CARD)
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public void markSuccess(String externalRef) {
        this.status = PaymentStatus.SUCCESS;
        this.externalRef = externalRef;
        this.processedAt = LocalDateTime.now();
    }

    public void markFailed(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
        this.processedAt = LocalDateTime.now();
    }

    public void incrementRetry() { this.retryCount++; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private UUID subscriptionId;
        private UUID userId;
        private String idempotencyKey;
        private BigDecimal amount;
        private String currency;
        private PaymentStatus status = PaymentStatus.PENDING;
        private PaymentType type;
        private PaymentMethod paymentMethod;
        private String externalRef;
        private String failureReason;
        private int retryCount;
        private LocalDateTime processedAt;
        private LocalDateTime createdAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder subscriptionId(UUID subscriptionId) { this.subscriptionId = subscriptionId; return this; }
        public Builder userId(UUID userId) { this.userId = userId; return this; }
        public Builder idempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; return this; }
        public Builder amount(BigDecimal amount) { this.amount = amount; return this; }
        public Builder currency(String currency) { this.currency = currency; return this; }
        public Builder status(PaymentStatus status) { this.status = status; return this; }
        public Builder type(PaymentType type) { this.type = type; return this; }
        public Builder paymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; return this; }
        public Builder externalRef(String externalRef) { this.externalRef = externalRef; return this; }
        public Builder failureReason(String failureReason) { this.failureReason = failureReason; return this; }
        public Builder retryCount(int retryCount) { this.retryCount = retryCount; return this; }
        public Builder processedAt(LocalDateTime processedAt) { this.processedAt = processedAt; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Payment build() { return new Payment(this); }
    }
}
