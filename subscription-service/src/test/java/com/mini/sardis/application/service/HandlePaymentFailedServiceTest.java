package com.mini.sardis.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mini.sardis.application.exception.SubscriptionNotFoundException;
import com.mini.sardis.application.port.out.OutboxRepositoryPort;
import com.mini.sardis.application.port.out.SubscriptionRepositoryPort;
import com.mini.sardis.application.service.subscription.HandlePaymentFailedService;
import com.mini.sardis.domain.entity.Subscription;
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
class HandlePaymentFailedServiceTest {

    @Mock private SubscriptionRepositoryPort subscriptionRepo;
    @Mock private OutboxRepositoryPort outboxRepo;

    private HandlePaymentFailedService service;

    @BeforeEach
    void setUp() {
        service = new HandlePaymentFailedService(subscriptionRepo, outboxRepo, new ObjectMapper());
    }

    @Test
    void initialPaymentFailure_cancelsPendingSubscription() {
        UUID subscriptionId = UUID.randomUUID();
        Subscription sub = pendingSubscription(subscriptionId);

        when(subscriptionRepo.findById(subscriptionId)).thenReturn(Optional.of(sub));
        when(subscriptionRepo.save(any())).thenReturn(sub);

        service.execute(subscriptionId, "INITIAL", "insufficient_funds");

        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);

        var captor = ArgumentCaptor.forClass(com.mini.sardis.domain.entity.OutboxEvent.class);
        verify(outboxRepo).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("subscription.failed.v1");
    }

    @Test
    void renewalPaymentFailure_suspendsActiveSubscription() {
        UUID subscriptionId = UUID.randomUUID();
        Subscription sub = activeSubscription(subscriptionId);

        when(subscriptionRepo.findById(subscriptionId)).thenReturn(Optional.of(sub));
        when(subscriptionRepo.save(any())).thenReturn(sub);

        service.execute(subscriptionId, "RENEWAL", "card_declined");

        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.SUSPENDED);

        var captor = ArgumentCaptor.forClass(com.mini.sardis.domain.entity.OutboxEvent.class);
        verify(outboxRepo).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("subscription.suspended.v1");
    }

    @Test
    void handleFailure_throwsWhenSubscriptionNotFound() {
        UUID subscriptionId = UUID.randomUUID();
        when(subscriptionRepo.findById(subscriptionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(subscriptionId, "INITIAL", "error"))
                .isInstanceOf(SubscriptionNotFoundException.class);
    }

    private Subscription pendingSubscription(UUID id) {
        return Subscription.builder()
                .id(id)
                .userId(UUID.randomUUID())
                .planId(UUID.randomUUID())
                .status(SubscriptionStatus.PENDING)
                .amount(BigDecimal.valueOf(99.99))
                .currency("TRY")
                .version(0)
                .build();
    }

    private Subscription activeSubscription(UUID id) {
        return Subscription.builder()
                .id(id)
                .userId(UUID.randomUUID())
                .planId(UUID.randomUUID())
                .status(SubscriptionStatus.ACTIVE)
                .amount(BigDecimal.valueOf(99.99))
                .currency("TRY")
                .version(0)
                .build();
    }
}
