package com.mini.sardis.application.port.in.subscription;

import java.util.UUID;

public interface ActivateSubscriptionUseCase {
    void execute(UUID subscriptionId, String paymentType);
}
