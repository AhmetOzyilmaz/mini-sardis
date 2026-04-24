package com.mini.sardis.application.port.in.subscription;

import java.util.UUID;

public interface HandlePaymentFailedUseCase {
    void execute(UUID subscriptionId, String paymentType, String reason);
}
