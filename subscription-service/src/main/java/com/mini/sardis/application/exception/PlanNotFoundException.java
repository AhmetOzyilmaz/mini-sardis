package com.mini.sardis.application.exception;

import java.util.UUID;

public class PlanNotFoundException extends RuntimeException {
    public PlanNotFoundException(UUID id) {
        super("Subscription plan not found or inactive: " + id);
    }
}
