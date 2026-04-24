package com.mini.sardis.application.port.out;

import com.mini.sardis.domain.entity.Subscription;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepositoryPort {
    Subscription save(Subscription subscription);
    Optional<Subscription> findById(UUID id);
    List<Subscription> findByUserId(UUID userId);
    List<Subscription> findAll();
    List<Subscription> findDueForRenewal(LocalDate asOf);
}
