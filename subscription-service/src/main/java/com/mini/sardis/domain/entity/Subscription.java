package com.mini.sardis.domain.entity;

import com.mini.sardis.domain.value.SubscriptionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class Subscription {

    private final UUID id;
    private final UUID userId;
    private final UUID planId;
    private SubscriptionStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate nextRenewalDate;
    private LocalDateTime cancelledAt;
    private String cancellationReason;
    private final int version;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Price snapshot so Payment Service knows what to charge
    private final BigDecimal amount;
    private final String currency;
    private final UUID promoCodeId;
    private final BigDecimal discountAmount;
    private final BigDecimal finalAmount;

    private Subscription(Builder b) {
        this.id = b.id;
        this.userId = b.userId;
        this.planId = b.planId;
        this.status = b.status;
        this.startDate = b.startDate;
        this.endDate = b.endDate;
        this.nextRenewalDate = b.nextRenewalDate;
        this.cancelledAt = b.cancelledAt;
        this.cancellationReason = b.cancellationReason;
        this.version = b.version;
        this.createdAt = b.createdAt;
        this.updatedAt = b.updatedAt;
        this.amount = b.amount;
        this.currency = b.currency;
        this.promoCodeId = b.promoCodeId;
        this.discountAmount = b.discountAmount;
        this.finalAmount = b.finalAmount;
    }

    public static Subscription create(UUID userId, UUID planId, BigDecimal amount, String currency) {
        return new Builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .planId(planId)
                .status(SubscriptionStatus.PENDING)
                .amount(amount)
                .currency(currency)
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(amount)
                .version(0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static Subscription createWithPromo(UUID userId, UUID planId, BigDecimal amount,
                                               String currency, UUID promoCodeId,
                                               BigDecimal discountAmount) {
        BigDecimal finalAmt = amount.subtract(discountAmount).max(BigDecimal.ZERO);
        return new Builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .planId(planId)
                .status(SubscriptionStatus.PENDING)
                .amount(amount)
                .currency(currency)
                .promoCodeId(promoCodeId)
                .discountAmount(discountAmount)
                .finalAmount(finalAmt)
                .version(0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public void activate(int durationDays) {
        if (status != SubscriptionStatus.PENDING && status != SubscriptionStatus.SUSPENDED) {
            throw new IllegalStateException("Cannot activate from status: " + status);
        }
        this.status = SubscriptionStatus.ACTIVE;
        this.startDate = LocalDate.now();
        this.nextRenewalDate = LocalDate.now().plusDays(durationDays);
        this.updatedAt = LocalDateTime.now();
    }

    public void cancel(String reason) {
        if (status == SubscriptionStatus.CANCELLED) {
            throw new IllegalStateException("Subscription already cancelled");
        }
        this.status = SubscriptionStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.cancellationReason = reason;
        this.updatedAt = LocalDateTime.now();
    }

    public void suspend() {
        if (status != SubscriptionStatus.ACTIVE) {
            throw new IllegalStateException("Cannot suspend from status: " + status);
        }
        this.status = SubscriptionStatus.SUSPENDED;
        this.updatedAt = LocalDateTime.now();
    }

    public void extendRenewal(int durationDays) {
        this.nextRenewalDate = (nextRenewalDate != null ? nextRenewalDate : LocalDate.now())
                .plusDays(durationDays);
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isEligibleForRenewal() {
        return status == SubscriptionStatus.ACTIVE
                && nextRenewalDate != null
                && !nextRenewalDate.isAfter(LocalDate.now());
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getPlanId() { return planId; }
    public SubscriptionStatus getStatus() { return status; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public LocalDate getNextRenewalDate() { return nextRenewalDate; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public String getCancellationReason() { return cancellationReason; }
    public int getVersion() { return version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public UUID getPromoCodeId() { return promoCodeId; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public BigDecimal getFinalAmount() { return finalAmount; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private UUID userId;
        private UUID planId;
        private SubscriptionStatus status = SubscriptionStatus.PENDING;
        private LocalDate startDate;
        private LocalDate endDate;
        private LocalDate nextRenewalDate;
        private LocalDateTime cancelledAt;
        private String cancellationReason;
        private int version = 0;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private BigDecimal amount;
        private String currency;
        private UUID promoCodeId;
        private BigDecimal discountAmount;
        private BigDecimal finalAmount;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder userId(UUID userId) { this.userId = userId; return this; }
        public Builder planId(UUID planId) { this.planId = planId; return this; }
        public Builder status(SubscriptionStatus status) { this.status = status; return this; }
        public Builder startDate(LocalDate startDate) { this.startDate = startDate; return this; }
        public Builder endDate(LocalDate endDate) { this.endDate = endDate; return this; }
        public Builder nextRenewalDate(LocalDate nextRenewalDate) { this.nextRenewalDate = nextRenewalDate; return this; }
        public Builder cancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; return this; }
        public Builder cancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; return this; }
        public Builder version(int version) { this.version = version; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public Builder amount(BigDecimal amount) { this.amount = amount; return this; }
        public Builder currency(String currency) { this.currency = currency; return this; }
        public Builder promoCodeId(UUID promoCodeId) { this.promoCodeId = promoCodeId; return this; }
        public Builder discountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; return this; }
        public Builder finalAmount(BigDecimal finalAmount) { this.finalAmount = finalAmount; return this; }
        public Subscription build() { return new Subscription(this); }
    }
}
