package com.mini.sardis.application.service.renewal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mini.sardis.application.port.out.OutboxRepositoryPort;
import com.mini.sardis.application.port.out.SubscriptionRepositoryPort;
import com.mini.sardis.domain.entity.OutboxEvent;
import com.mini.sardis.domain.entity.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class RenewalScheduler {

    private static final Logger log = LoggerFactory.getLogger(RenewalScheduler.class);

    private final SubscriptionRepositoryPort subscriptionRepo;
    private final OutboxRepositoryPort outboxRepo;
    private final ObjectMapper objectMapper;

    public RenewalScheduler(SubscriptionRepositoryPort subscriptionRepo,
                             OutboxRepositoryPort outboxRepo,
                             ObjectMapper objectMapper) {
        this.subscriptionRepo = subscriptionRepo;
        this.outboxRepo = outboxRepo;
        this.objectMapper = objectMapper;
    }

    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void processRenewals() {
        List<Subscription> due = subscriptionRepo.findDueForRenewal(LocalDate.now());
        log.info("RenewalScheduler: {} subscriptions due for renewal", due.size());

        for (Subscription subscription : due) {
            OutboxEvent event = OutboxEvent.create(
                    subscription.getId(), "Subscription", "renewal.requested.v1",
                    buildPayload(subscription));
            outboxRepo.save(event);
        }
    }

    private String buildPayload(Subscription s) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "subscriptionId", s.getId().toString(),
                    "userId", s.getUserId().toString(),
                    "planId", s.getPlanId().toString(),
                    "amount", s.getAmount(),
                    "currency", s.getCurrency(),
                    "idempotencyKey", s.getId().toString() + "-" + LocalDate.now()
            ));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize renewal payload", e);
        }
    }
}
