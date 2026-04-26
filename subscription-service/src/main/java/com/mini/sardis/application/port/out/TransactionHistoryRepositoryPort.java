package com.mini.sardis.application.port.out;

import com.mini.sardis.domain.entity.OutboxEvent;

import java.util.List;
import java.util.UUID;

public interface TransactionHistoryRepositoryPort {
    List<OutboxEvent> findByAggregateId(UUID aggregateId);
    List<OutboxEvent> findByAggregateIds(List<UUID> aggregateIds);
}
