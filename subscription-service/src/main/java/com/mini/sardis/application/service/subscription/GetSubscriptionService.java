package com.mini.sardis.application.service.subscription;

import com.mini.sardis.application.exception.SubscriptionNotFoundException;
import com.mini.sardis.application.port.in.subscription.GetSubscriptionUseCase;
import com.mini.sardis.application.port.in.subscription.SubscriptionResult;
import com.mini.sardis.application.port.out.SubscriptionRepositoryPort;
import com.mini.sardis.domain.entity.Subscription;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class GetSubscriptionService implements GetSubscriptionUseCase {

    private final SubscriptionRepositoryPort subscriptionRepo;

    public GetSubscriptionService(SubscriptionRepositoryPort subscriptionRepo) {
        this.subscriptionRepo = subscriptionRepo;
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionResult findById(UUID id) {
        return subscriptionRepo.findById(id)
                .map(this::toResult)
                .orElseThrow(() -> new SubscriptionNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionResult> findByUserId(UUID userId) {
        return subscriptionRepo.findByUserId(userId).stream().map(this::toResult).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionResult> findAll() {
        return subscriptionRepo.findAll().stream().map(this::toResult).toList();
    }

    private SubscriptionResult toResult(Subscription s) {
        return new SubscriptionResult(s.getId(), s.getUserId(), s.getPlanId(),
                s.getStatus(), s.getStartDate(), s.getNextRenewalDate(), s.getCreatedAt(),
                s.getAmount(), s.getDiscountAmount(), s.getFinalAmount());
    }
}
