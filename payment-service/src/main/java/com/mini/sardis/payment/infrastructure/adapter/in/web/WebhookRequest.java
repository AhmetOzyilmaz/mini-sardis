package com.mini.sardis.payment.infrastructure.adapter.in.web;

import jakarta.validation.constraints.NotBlank;

public record WebhookRequest(
        @NotBlank String idempotencyKey,
        String externalRef,
        boolean success,
        String failureReason
) {}
