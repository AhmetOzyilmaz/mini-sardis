package com.mini.sardis.infrastructure.adapter.out.jpa;

import com.mini.sardis.application.port.out.OutboxRepositoryPort;
import com.mini.sardis.domain.entity.OutboxEvent;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JpaOutboxRepositoryAdapter implements OutboxRepositoryPort {

    private final JpaOutboxRepository jpaRepo;

    public JpaOutboxRepositoryAdapter(JpaOutboxRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public void save(OutboxEvent event) {
        jpaRepo.save(toJpa(event));
    }

    @Override
    public List<OutboxEvent> findUnprocessed(int limit) {
        return jpaRepo.findUnprocessed(limit).stream().map(this::toDomain).toList();
    }

    @Override
    public void markProcessed(OutboxEvent event) {
        event.markProcessed();
        jpaRepo.save(toJpa(event));
    }

    private OutboxEventJpaEntity toJpa(OutboxEvent e) {
        return OutboxEventJpaEntity.builder()
                .id(e.getId())
                .aggregateId(e.getAggregateId())
                .aggregateType(e.getAggregateType())
                .eventType(e.getEventType())
                .payload(e.getPayload())
                .processed(e.isProcessed())
                .createdAt(e.getCreatedAt())
                .processedAt(e.getProcessedAt())
                .build();
    }

    private OutboxEvent toDomain(OutboxEventJpaEntity e) {
        return OutboxEvent.reconstruct(e.getId(), e.getAggregateId(), e.getAggregateType(),
                e.getEventType(), e.getPayload(), e.isProcessed(), e.getCreatedAt(), e.getProcessedAt());
    }
}
