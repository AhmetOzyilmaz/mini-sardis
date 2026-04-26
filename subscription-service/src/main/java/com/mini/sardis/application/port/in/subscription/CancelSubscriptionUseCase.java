package com.mini.sardis.application.port.in.subscription;

import java.util.UUID;

public interface CancelSubscriptionUseCase {
    void execute(UUID subscriptionId, UUID requestingUserId, String reason, boolean cancelAtPeriodEnd);

    default void execute(UUID subscriptionId, UUID requestingUserId, String reason) {
        execute(subscriptionId, requestingUserId, reason, false);
    }
}
