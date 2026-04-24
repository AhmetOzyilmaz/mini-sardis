package com.mini.sardis.application.port.in.subscription;

public interface CreateSubscriptionUseCase {
    SubscriptionResult execute(CreateSubscriptionCommand command);
}
