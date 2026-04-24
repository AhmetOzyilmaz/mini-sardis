package com.mini.sardis.infrastructure.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface JpaOutboxRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {

    @Query("SELECT e FROM OutboxEventJpaEntity e WHERE e.processed = false ORDER BY e.createdAt ASC LIMIT :limit")
    List<OutboxEventJpaEntity> findUnprocessed(@Param("limit") int limit);
}
