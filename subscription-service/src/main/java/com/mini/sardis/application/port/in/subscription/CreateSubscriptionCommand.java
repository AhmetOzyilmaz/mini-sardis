package com.mini.sardis.application.port.in.subscription;

import java.util.UUID;

public record CreateSubscriptionCommand(UUID userId, UUID planId, String promoCode) {
    public CreateSubscriptionCommand(UUID userId, UUID planId) {
        this(userId, planId, null);
    }
}
