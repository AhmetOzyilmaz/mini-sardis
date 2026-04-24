package com.mini.sardis.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mini.sardis.application.exception.SubscriptionNotFoundException;
import com.mini.sardis.application.port.out.OutboxRepositoryPort;
import com.mini.sardis.application.port.out.SubscriptionRepositoryPort;
import com.mini.sardis.application.service.subscription.CancelSubscriptionService;
import com.mini.sardis.domain.entity.Subscription;
import com.mini.sardis.domain.value.SubscriptionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CancelSubscriptionServiceTest {

    @Mock private SubscriptionRepositoryPort subscriptionRepo;
    @Mock private OutboxRepositoryPort outboxRepo;

    private CancelSubscriptionService service;

    @BeforeEach
    void setUp() {
        service = new CancelSubscriptionService(subscriptionRepo, outboxRepo, new ObjectMapper());
    }

    @Test
    void cancel_savesAndPublishesEvent() {
        UUID userId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        Subscription sub = activeSubscription(subscriptionId, userId);

        when(subscriptionRepo.findById(subscriptionId)).thenReturn(Optional.of(sub));
        when(subscriptionRepo.save(any())).thenReturn(sub);

        service.execute(subscriptionId, userId, "user_request");

        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        var captor = ArgumentCaptor.forClass(com.mini.sardis.domain.entity.OutboxEvent.class);
        verify(outboxRepo).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("subscription.cancelled.v1");
    }

    @Test
    void cancel_throwsWhenSubscriptionNotFound() {
        UUID subscriptionId = UUID.randomUUID();
        when(subscriptionRepo.findById(subscriptionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(subscriptionId, UUID.randomUUID(), "reason"))
                .isInstanceOf(SubscriptionNotFoundException.class);
    }

    @Test
    void cancel_throwsWhenDifferentUser() {
        UUID ownerId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        Subscription sub = activeSubscription(subscriptionId, ownerId);

        when(subscriptionRepo.findById(subscriptionId)).thenReturn(Optional.of(sub));

        assertThatThrownBy(() -> service.execute(subscriptionId, otherId, "reason"))
                .isInstanceOf(AccessDeniedException.class);

        verify(subscriptionRepo, never()).save(any());
        verify(outboxRepo, never()).save(any());
    }

    @Test
    void cancel_throwsWhenAlreadyCancelled() {
        UUID userId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        Subscription sub = cancelledSubscription(subscriptionId, userId);

        when(subscriptionRepo.findById(subscriptionId)).thenReturn(Optional.of(sub));

        assertThatThrownBy(() -> service.execute(subscriptionId, userId, "reason"))
                .isInstanceOf(IllegalStateException.class);
    }

    private Subscription activeSubscription(UUID id, UUID userId) {
        return Subscription.builder()
                .id(id)
                .userId(userId)
                .planId(UUID.randomUUID())
                .status(SubscriptionStatus.ACTIVE)
                .amount(BigDecimal.valueOf(99.99))
                .currency("TRY")
                .version(0)
                .build();
    }

    private Subscription cancelledSubscription(UUID id, UUID userId) {
        return Subscription.builder()
                .id(id)
                .userId(userId)
                .planId(UUID.randomUUID())
                .status(SubscriptionStatus.CANCELLED)
                .amount(BigDecimal.valueOf(99.99))
                .currency("TRY")
                .version(0)
                .build();
    }
}
