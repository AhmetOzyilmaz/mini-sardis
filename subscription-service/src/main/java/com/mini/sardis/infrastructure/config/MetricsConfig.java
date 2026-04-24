package com.mini.sardis.infrastructure.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    // Common tags (application, environment) are configured via
    // management.metrics.tags.* in application.properties — no code needed here.

    // Outbox lag gauge — add when OutboxPoller is implemented:
    //
    // @Bean
    // public Gauge outboxLagGauge(OutboxEventRepository repo, MeterRegistry registry) {
    //     return Gauge.builder("outbox.events.unprocessed", repo, r -> r.countByProcessedFalse())
    //                 .description("Unprocessed outbox events awaiting Kafka publish")
    //                 .register(registry);
    // }

    // Renewal batch size gauge — add when RenewalScheduler is implemented:
    //
    // @Bean
    // public Gauge renewalBatchGauge(AtomicInteger batchSize, MeterRegistry registry) { ... }
}
