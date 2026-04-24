package com.mini.sardis.notification.infrastructure.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {
    // Common metrics tags (application, environment) are set via
    // management.metrics.tags.* in application.properties.
    // Custom notification counters can be registered here via MeterRegistry injection.
}
