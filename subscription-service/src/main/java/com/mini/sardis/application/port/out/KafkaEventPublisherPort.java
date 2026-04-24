package com.mini.sardis.application.port.out;

public interface KafkaEventPublisherPort {
    void publish(String topic, String key, String payload);
}
