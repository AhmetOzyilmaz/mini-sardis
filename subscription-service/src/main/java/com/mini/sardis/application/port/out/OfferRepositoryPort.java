package com.mini.sardis.application.port.out;

import com.mini.sardis.domain.entity.Offer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OfferRepositoryPort {
    Offer save(Offer offer);
    Optional<Offer> findById(UUID id);
    List<Offer> findAllActive();
    List<Offer> findActiveByTargetUserId(UUID userId);
}
