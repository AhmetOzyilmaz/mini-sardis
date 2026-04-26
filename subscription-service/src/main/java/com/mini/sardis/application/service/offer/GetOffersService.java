package com.mini.sardis.application.service.offer;

import com.mini.sardis.application.exception.OfferNotFoundException;
import com.mini.sardis.application.port.in.offer.GetOffersUseCase;
import com.mini.sardis.application.port.in.offer.OfferResult;
import com.mini.sardis.application.port.out.OfferRepositoryPort;
import com.mini.sardis.application.port.out.SubscriptionRepositoryPort;
import com.mini.sardis.domain.entity.Offer;
import com.mini.sardis.domain.entity.Subscription;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class GetOffersService implements GetOffersUseCase {

    private final OfferRepositoryPort offerRepo;
    private final SubscriptionRepositoryPort subscriptionRepo;

    @Override
    @Transactional(readOnly = true)
    public List<OfferResult> findEligibleForUser(UUID userId) {
        Set<UUID> currentPlanIds = subscriptionRepo.findByUserId(userId).stream()
                .map(Subscription::getPlanId)
                .collect(Collectors.toSet());

        List<Offer> candidates = Stream.concat(
                offerRepo.findAllActive().stream(),
                offerRepo.findActiveByTargetUserId(userId).stream()
        ).distinct().toList();

        UUID anyCurrentPlan = currentPlanIds.isEmpty() ? null : currentPlanIds.iterator().next();

        return candidates.stream()
                .filter(o -> o.isEligibleFor(userId, anyCurrentPlan))
                .map(OfferResult::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OfferResult findById(UUID offerId) {
        return offerRepo.findById(offerId)
                .map(OfferResult::from)
                .orElseThrow(() -> new OfferNotFoundException(offerId));
    }
}
