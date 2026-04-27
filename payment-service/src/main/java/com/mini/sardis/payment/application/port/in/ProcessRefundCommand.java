package com.mini.sardis.payment.application.port.in;

import java.util.UUID;

public record ProcessRefundCommand(UUID subscriptionId, UUID userId, String reason) {}
