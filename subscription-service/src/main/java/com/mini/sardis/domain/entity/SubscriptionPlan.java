package com.mini.sardis.domain.entity;

import com.mini.sardis.domain.value.Money;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class SubscriptionPlan {

    private final UUID id;
    private final String name;
    private final String description;
    private final Money price;
    private final int durationDays;
    private final int trialDays;
    private final boolean active;
    private final LocalDateTime createdAt;

    private SubscriptionPlan(Builder b) {
        this.id = b.id;
        this.name = b.name;
        this.description = b.description;
        this.price = b.price;
        this.durationDays = b.durationDays;
        this.trialDays = b.trialDays;
        this.active = b.active;
        this.createdAt = b.createdAt;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private String name;
        private String description;
        private Money price;
        private int durationDays = 30;
        private int trialDays = 0;
        private boolean active = true;
        private LocalDateTime createdAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder price(Money price) { this.price = price; return this; }
        public Builder price(BigDecimal amount, String currency) { this.price = Money.of(amount, currency); return this; }
        public Builder durationDays(int durationDays) { this.durationDays = durationDays; return this; }
        public Builder trialDays(int trialDays) { this.trialDays = trialDays; return this; }
        public Builder active(boolean active) { this.active = active; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public SubscriptionPlan build() { return new SubscriptionPlan(this); }
    }
}
