package com.mini.sardis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class KafkaTestConsumerImpl {

    private final List<String> receivedMessages = new ArrayList<>();

    public void receive(String message, String topic) {
        log.info("Test consumer captured message on topic={}", topic);
        receivedMessages.add(message);
    }

    public List<String> getReceivedMessages() {
        return List.copyOf(receivedMessages);
    }

    public void clear() {
        receivedMessages.clear();
    }
}
