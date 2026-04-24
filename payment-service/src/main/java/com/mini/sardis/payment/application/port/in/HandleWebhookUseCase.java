package com.mini.sardis.payment.application.port.in;

public interface HandleWebhookUseCase {
    void execute(HandleWebhookCommand command);
}
