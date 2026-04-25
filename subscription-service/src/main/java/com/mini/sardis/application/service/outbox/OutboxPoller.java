package com.mini.sardis.application.service.outbox;

import com.mini.sardis.application.port.out.KafkaEventPublisherPort;
import com.mini.sardis.application.port.out.OutboxRepositoryPort;
import com.mini.sardis.domain.entity.OutboxEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class OutboxPoller {

    private static final int BATCH_SIZE = 100;

    private final OutboxRepositoryPort outboxRepo;
    private final KafkaEventPublisherPort kafkaPublisher;

    public OutboxPoller(OutboxRepositoryPort outboxRepo, KafkaEventPublisherPort kafkaPublisher) {
        this.outboxRepo = outboxRepo;
        this.kafkaPublisher = kafkaPublisher;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void poll() {
        List<OutboxEvent> events = outboxRepo.findUnprocessed(BATCH_SIZE);
        if (events.isEmpty()) return;

        log.debug("OutboxPoller: processing {} events", events.size());
        for (OutboxEvent event : events) {
            try {
                kafkaPublisher.publish(event.getEventType(),
                        event.getAggregateId().toString(),
                        event.getPayload());
                outboxRepo.markProcessed(event);
            } catch (Exception e) {
                log.error("Failed to publish outbox event {}: {}", event.getId(), e.getMessage());
            }
        }
    }
}
