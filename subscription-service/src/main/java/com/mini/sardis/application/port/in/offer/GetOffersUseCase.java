package com.mini.sardis.application.port.in.offer;

import java.util.List;
import java.util.UUID;

public interface GetOffersUseCase {
    List<OfferResult> findEligibleForUser(UUID userId);
    OfferResult findById(UUID offerId);
}
