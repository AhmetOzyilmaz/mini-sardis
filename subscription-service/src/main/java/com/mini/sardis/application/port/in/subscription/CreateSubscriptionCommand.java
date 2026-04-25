package com.mini.sardis.application.port.in.subscription;

import java.util.UUID;

public record CreateSubscriptionCommand(UUID userId, UUID planId, String promoCode, String paymentMethod) {
    public CreateSubscriptionCommand(UUID userId, UUID planId) {
        this(userId, planId, null, null);
    }
    public CreateSubscriptionCommand(UUID userId, UUID planId, String promoCode) {
        this(userId, planId, promoCode, null);
    }
}
