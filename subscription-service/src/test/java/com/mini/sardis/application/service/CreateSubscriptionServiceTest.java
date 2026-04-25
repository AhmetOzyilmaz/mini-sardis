package com.mini.sardis.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mini.sardis.application.exception.PlanNotFoundException;
import com.mini.sardis.application.port.in.subscription.CreateSubscriptionCommand;
import com.mini.sardis.application.port.in.subscription.SubscriptionResult;
import com.mini.sardis.application.port.out.OutboxRepositoryPort;
import com.mini.sardis.application.port.out.PromoCodeRepositoryPort;
import com.mini.sardis.application.port.out.SubscriptionPlanRepositoryPort;
import com.mini.sardis.application.port.out.SubscriptionRepositoryPort;
import com.mini.sardis.application.service.subscription.CreateSubscriptionService;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateSubscriptionServiceTest {

    @Mock private SubscriptionRepositoryPort subscriptionRepo;
    @Mock private SubscriptionPlanRepositoryPort planRepo;
    @Mock private OutboxRepositoryPort outboxRepo;
    @Mock private PromoCodeRepositoryPort promoCodeRepo;

    private CreateSubscriptionService service;

    @BeforeEach
    void setUp() {
        service = new CreateSubscriptionService(subscriptionRepo, planRepo, outboxRepo,
                new ObjectMapper(), promoCodeRepo);
    }

    @Test
    void createSubscription_savesSubscriptionAndOutboxEvent() {
        UUID userId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        SubscriptionPlan plan = buildActivePlan(planId);

        when(planRepo.findById(planId)).thenReturn(Optional.of(plan));
        when(subscriptionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SubscriptionResult result = service.execute(new CreateSubscriptionCommand(userId, planId));

        assertThat(result.status()).isEqualTo(SubscriptionStatus.PENDING);
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.planId()).isEqualTo(planId);

        verify(outboxRepo).save(any());
    }

    @Test
    void createSubscription_throwsWhenPlanNotFound() {
        UUID planId = UUID.randomUUID();
        when(planRepo.findById(planId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(new CreateSubscriptionCommand(UUID.randomUUID(), planId)))
                .isInstanceOf(PlanNotFoundException.class);

        verify(subscriptionRepo, never()).save(any());
        verify(outboxRepo, never()).save(any());
    }

    @Test
    void createSubscription_throwsWhenPlanInactive() {
        UUID planId = UUID.randomUUID();
        SubscriptionPlan inactivePlan = buildInactivePlan(planId);

        when(planRepo.findById(planId)).thenReturn(Optional.of(inactivePlan));

        assertThatThrownBy(() -> service.execute(new CreateSubscriptionCommand(UUID.randomUUID(), planId)))
                .isInstanceOf(PlanNotFoundException.class);
    }

    @Test
    void createSubscription_outboxEventHasCorrectType() {
        UUID userId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        when(planRepo.findById(planId)).thenReturn(Optional.of(buildActivePlan(planId)));
        when(subscriptionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.execute(new CreateSubscriptionCommand(userId, planId));

        var captor = ArgumentCaptor.forClass(com.mini.sardis.domain.entity.OutboxEvent.class);
        verify(outboxRepo).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("subscription.created.v1");
    }

    private SubscriptionPlan buildActivePlan(UUID planId) {
        return SubscriptionPlan.builder()
                .id(planId)
                .name("Pro")
                .price(BigDecimal.valueOf(99.99), "TRY")
                .durationDays(30)
                .active(true)
                .build();
    }

    private SubscriptionPlan buildInactivePlan(UUID planId) {
        return SubscriptionPlan.builder()
                .id(planId)
                .name("Old")
                .price(BigDecimal.valueOf(99.99), "TRY")
                .durationDays(30)
                .active(false)
                .build();
    }
}
