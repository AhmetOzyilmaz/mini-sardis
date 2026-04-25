package com.mini.sardis.common.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {
    // Common metrics tags are configured via management.metrics.tags.* in
    // each service's application.properties. Service-specific gauges live
    // in each service's own MetricsConfig which supplements this class.
}
