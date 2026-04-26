package com.mini.sardis.application.port.in.subscription;

import java.util.UUID;

public interface ReactivateSubscriptionUseCase {
    SubscriptionResult execute(UUID subscriptionId, UUID requestingUserId);
}
