package com.mini.sardis.application.port.in.subscription;

import java.util.UUID;

public record RequestRefundCommand(UUID subscriptionId, UUID requestingUserId, String reason) {}
