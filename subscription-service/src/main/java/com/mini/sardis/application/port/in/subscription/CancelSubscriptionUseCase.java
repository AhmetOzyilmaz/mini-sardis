package com.mini.sardis.application.port.in.subscription;

import java.util.UUID;

public interface CancelSubscriptionUseCase {
    void execute(UUID subscriptionId, UUID requestingUserId, String reason);
}
