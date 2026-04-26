package com.mini.sardis.application.service.grace;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mini.sardis.application.port.out.OutboxRepositoryPort;
import com.mini.sardis.application.port.out.SubscriptionRepositoryPort;
import com.mini.sardis.domain.entity.OutboxEvent;
import com.mini.sardis.domain.entity.Subscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GracePeriodExpiryScheduler {

    private final SubscriptionRepositoryPort subscriptionRepo;
    private final OutboxRepositoryPort outboxRepo;
    private final ObjectMapper objectMapper;

    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void expireGracePeriods() {
        List<Subscription> expired = subscriptionRepo.findExpiredGracePeriods(LocalDate.now());
        log.info("GracePeriodExpiryScheduler: {} expired grace periods to cancel", expired.size());

        for (Subscription subscription : expired) {
            subscription.cancel("grace_period_expired");
            subscriptionRepo.save(subscription);
            outboxRepo.save(OutboxEvent.create(
                    subscription.getId(), "Subscription", "subscription.cancelled.v1",
                    buildPayload(subscription)));
        }
    }

    private String buildPayload(Subscription s) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "subscriptionId", s.getId().toString(),
                    "userId", s.getUserId().toString(),
                    "reason", "grace_period_expired"
            ));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize outbox payload", e);
        }
    }
}
