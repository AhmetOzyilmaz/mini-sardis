package com.mini.sardis.application.port.out;

import com.mini.sardis.domain.entity.OutboxEvent;

import java.util.List;

public interface OutboxRepositoryPort {
    void save(OutboxEvent event);
    List<OutboxEvent> findUnprocessed(int limit);
    void markProcessed(OutboxEvent event);
}
