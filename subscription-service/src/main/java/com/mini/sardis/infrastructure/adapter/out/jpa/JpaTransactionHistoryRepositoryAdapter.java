package com.mini.sardis.infrastructure.adapter.out.jpa;

import com.mini.sardis.application.port.out.TransactionHistoryRepositoryPort;
import com.mini.sardis.domain.entity.OutboxEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JpaTransactionHistoryRepositoryAdapter implements TransactionHistoryRepositoryPort {

    private final JpaOutboxRepository jpaRepo;

    @Override
    public List<OutboxEvent> findByAggregateId(UUID aggregateId) {
        return jpaRepo.findByAggregateIdOrderByCreatedAtDesc(aggregateId)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<OutboxEvent> findByAggregateIds(List<UUID> aggregateIds) {
        return jpaRepo.findByAggregateIdIn(aggregateIds)
                .stream().map(this::toDomain).toList();
    }

    private OutboxEvent toDomain(OutboxEventJpaEntity e) {
        return OutboxEvent.reconstruct(e.getId(), e.getAggregateId(), e.getAggregateType(),
                e.getEventType(), e.getPayload(), e.isProcessed(), e.getCreatedAt(), e.getProcessedAt());
    }
}
