package com.mini.sardis.domain.entity;

import com.mini.sardis.application.exception.InvalidPromoCodeException;
import com.mini.sardis.domain.value.DiscountType;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Getter
public class PromoCode {

    private final UUID id;
    private final String code;
    private final DiscountType discountType;
    private final BigDecimal discountValue;
    private final Integer maxUses;
    private int currentUses;
    private final boolean active;
    private final LocalDateTime validFrom;
    private final LocalDateTime validTo;
    private final LocalDateTime createdAt;
    private final Set<Integer> applicableMonths;

    private PromoCode(Builder b) {
        this.id = b.id;
        this.code = b.code;
        this.discountType = b.discountType;
        this.discountValue = b.discountValue;
        this.maxUses = b.maxUses;
        this.currentUses = b.currentUses;
        this.active = b.active;
        this.validFrom = b.validFrom;
        this.validTo = b.validTo;
        this.createdAt = b.createdAt;
        this.applicableMonths = b.applicableMonths;
    }

    public static PromoCode create(String code, DiscountType discountType, BigDecimal discountValue,
                                   Integer maxUses, LocalDateTime validFrom, LocalDateTime validTo) {
        return create(code, discountType, discountValue, maxUses, validFrom, validTo, null);
    }

    public static PromoCode create(String code, DiscountType discountType, BigDecimal discountValue,
                                   Integer maxUses, LocalDateTime validFrom, LocalDateTime validTo,
                                   Set<Integer> applicableMonths) {
        return new Builder()
                .id(UUID.randomUUID())
                .code(code.toUpperCase())
                .discountType(discountType)
                .discountValue(discountValue)
                .maxUses(maxUses)
                .currentUses(0)
                .active(true)
                .validFrom(validFrom)
                .validTo(validTo)
                .createdAt(LocalDateTime.now())
                .applicableMonths(applicableMonths)
                .build();
    }

    public void validate() {
        if (!active) {
            throw new InvalidPromoCodeException("Promo code '" + code + "' is not active");
        }
        LocalDateTime now = LocalDateTime.now();
        if (validFrom != null && now.isBefore(validFrom)) {
            throw new InvalidPromoCodeException("Promo code '" + code + "' is not yet valid");
        }
        if (validTo != null && now.isAfter(validTo)) {
            throw new InvalidPromoCodeException("Promo code '" + code + "' has expired");
        }
        if (maxUses != null && currentUses >= maxUses) {
            throw new InvalidPromoCodeException("Promo code '" + code + "' has reached its usage limit");
        }
    }

    public void validate(int durationMonths) {
        validate();
        if (applicableMonths != null && !applicableMonths.isEmpty()
                && !applicableMonths.contains(durationMonths)) {
            throw new InvalidPromoCodeException(
                    "Promo code '" + code + "' is not applicable for " + durationMonths
                            + "-month subscriptions. Applicable: " + applicableMonths);
        }
    }

    public BigDecimal calculateDiscountAmount(BigDecimal originalAmount) {
        BigDecimal discount;
        if (discountType == DiscountType.PERCENTAGE) {
            discount = originalAmount
                    .multiply(discountValue)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            discount = discountValue;
        }
        return discount.min(originalAmount).max(BigDecimal.ZERO);
    }

    public void incrementUse() {
        this.currentUses += 1;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private String code;
        private DiscountType discountType;
        private BigDecimal discountValue;
        private Integer maxUses;
        private int currentUses = 0;
        private boolean active = true;
        private LocalDateTime validFrom;
        private LocalDateTime validTo;
        private LocalDateTime createdAt;
        private Set<Integer> applicableMonths;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder code(String code) { this.code = code; return this; }
        public Builder discountType(DiscountType t) { this.discountType = t; return this; }
        public Builder discountValue(BigDecimal v) { this.discountValue = v; return this; }
        public Builder maxUses(Integer maxUses) { this.maxUses = maxUses; return this; }
        public Builder currentUses(int currentUses) { this.currentUses = currentUses; return this; }
        public Builder active(boolean active) { this.active = active; return this; }
        public Builder validFrom(LocalDateTime validFrom) { this.validFrom = validFrom; return this; }
        public Builder validTo(LocalDateTime validTo) { this.validTo = validTo; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder applicableMonths(Set<Integer> applicableMonths) { this.applicableMonths = applicableMonths; return this; }
        public PromoCode build() { return new PromoCode(this); }
    }
}
