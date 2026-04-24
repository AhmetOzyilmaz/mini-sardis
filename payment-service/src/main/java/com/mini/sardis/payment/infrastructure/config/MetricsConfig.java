package com.mini.sardis.payment.infrastructure.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {
    // Common metrics tags (application, environment) are set via
    // management.metrics.tags.* in application.properties.
    // Payment success/failure rate counters can be registered here via MeterRegistry injection.
}
