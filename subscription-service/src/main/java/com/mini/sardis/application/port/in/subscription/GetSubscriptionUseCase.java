package com.mini.sardis.application.port.in.subscription;

import java.util.List;
import java.util.UUID;

public interface GetSubscriptionUseCase {
    SubscriptionResult findById(UUID id);
    List<SubscriptionResult> findByUserId(UUID userId);
    List<SubscriptionResult> findAll();
}
