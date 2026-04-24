package com.mini.sardis.payment.application.port.in;

public record HandleWebhookCommand(
        String idempotencyKey,
        String externalRef,
        boolean success,
        String failureReason
) {}
