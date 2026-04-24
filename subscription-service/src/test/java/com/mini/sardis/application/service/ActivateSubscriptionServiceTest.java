package com.mini.sardis.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mini.sardis.application.exception.SubscriptionNotFoundException;
import com.mini.sardis.application.port.out.OutboxRepositoryPort;
import com.mini.sardis.application.port.out.SubscriptionPlanRepositoryPort;
import com.mini.sardis.application.port.out.SubscriptionRepositoryPort;
import com.mini.sardis.application.service.subscription.ActivateSubscriptionService;
import com.mini.sardis.domain.entity.Subscription;
import com.mini.sardis.domain.entity.SubscriptionPlan;
import com.mini.sardis.domain.value.SubscriptionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivateSubscriptionServiceTest {

    @Mock private SubscriptionRepositoryPort subscriptionRepo;
    @Mock private SubscriptionPlanRepositoryPort planRepo;
    @Mock private OutboxRepositoryPort outboxRepo;

    private ActivateSubscriptionService service;

    @BeforeEach
    void setUp() {
        service = new ActivateSubscriptionService(subscriptionRepo, planRepo, outboxRepo, new ObjectMapper());
    }

    @Test
    void initialPayment_activatesSubscription() {
        UUID subscriptionId = UUID.randomUUID();
        Subscription sub = pendingSubscription(subscriptionId);
        SubscriptionPlan plan = buildPlan(sub.getPlanId());

        when(subscriptionRepo.findById(subscriptionId)).thenReturn(Optional.of(sub));
        when(planRepo.findById(sub.getPlanId())).thenReturn(Optional.of(plan));
        when(subscriptionRepo.save(any())).thenReturn(sub);

        service.execute(subscriptionId, "INITIAL");

        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(sub.getNextRenewalDate()).isEqualTo(LocalDate.now().plusDays(30));

        var captor = ArgumentCaptor.forClass(com.mini.sardis.domain.entity.OutboxEvent.class);
        verify(outboxRepo).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("subscription.activated.v1");
    }

    @Test
    void renewalPayment_extendsRenewalDate() {
        UUID subscriptionId = UUID.randomUUID();
        Subscription sub = activeSubscription(subscriptionId);
        LocalDate originalRenewal = sub.getNextRenewalDate();
        SubscriptionPlan plan = buildPlan(sub.getPlanId());

        when(subscriptionRepo.findById(subscriptionId)).thenReturn(Optional.of(sub));
        when(planRepo.findById(sub.getPlanId())).thenReturn(Optional.of(plan));
        when(subscriptionRepo.save(any())).thenReturn(sub);

        service.execute(subscriptionId, "RENEWAL");

        assertThat(sub.getNextRenewalDate()).isEqualTo(originalRenewal.plusDays(30));
        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);

        var captor = ArgumentCaptor.forClass(com.mini.sardis.domain.entity.OutboxEvent.class);
        verify(outboxRepo).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("subscription.renewed.v1");
    }

    @Test
    void activate_throwsWhenSubscriptionNotFound() {
        UUID subscriptionId = UUID.randomUUID();
        when(subscriptionRepo.findById(subscriptionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(subscriptionId, "INITIAL"))
                .isInstanceOf(SubscriptionNotFoundException.class);
    }

    private Subscription pendingSubscription(UUID id) {
        UUID planId = UUID.randomUUID();
        return Subscription.builder()
                .id(id)
                .userId(UUID.randomUUID())
                .planId(planId)
                .status(SubscriptionStatus.PENDING)
                .amount(BigDecimal.valueOf(99.99))
                .currency("TRY")
                .version(0)
                .build();
    }

    private Subscription activeSubscription(UUID id) {
        UUID planId = UUID.randomUUID();
        return Subscription.builder()
                .id(id)
                .userId(UUID.randomUUID())
                .planId(planId)
                .status(SubscriptionStatus.ACTIVE)
                .nextRenewalDate(LocalDate.now())
                .amount(BigDecimal.valueOf(99.99))
                .currency("TRY")
                .version(0)
                .build();
    }

    private SubscriptionPlan buildPlan(UUID planId) {
        return SubscriptionPlan.builder()
                .id(planId)
                .name("Pro")
                .price(BigDecimal.valueOf(99.99), "TRY")
                .durationDays(30)
                .active(true)
                .build();
    }
}
