package com.mini.sardis.common.config;

import com.mini.sardis.common.security.WebhookSignatureVerifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import({JacksonConfig.class, KafkaConsumerConfig.class, KafkaProducerConfig.class, MetricsConfig.class})
public class SardisCommonAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(WebhookSignatureVerifier.class)
    public WebhookSignatureVerifier webhookSignatureVerifier() {
        return new WebhookSignatureVerifier();
    }
}
