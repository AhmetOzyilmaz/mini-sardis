package com.mini.sardis.infrastructure.adapter.in.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mini.sardis.application.exception.SubscriptionNotFoundException;
import com.mini.sardis.application.port.in.subscription.ActivateSubscriptionUseCase;
import com.mini.sardis.application.port.in.subscription.HandlePaymentFailedUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Real-world failure scenarios for the Kafka payment event listener.
 * Tests that the listener is resilient to downstream errors and malformed payloads.
 */
@ExtendWith(MockitoExtension.class)
class PaymentEventListenerRealWorldTest {

    @Mock private ActivateSubscriptionUseCase activateUseCase;
    @Mock private HandlePaymentFailedUseCase handleFailedUseCase;

    private PaymentEventListener listener;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        listener = new PaymentEventListener(activateUseCase, handleFailedUseCase, objectMapper);
    }

    @Test
    void completedEvent_subscriptionNotFound_doesNotThrow() {
        doThrow(new SubscriptionNotFoundException(UUID.randomUUID()))
                .when(activateUseCase).execute(any(), any());

        String payload = buildCompletedPayload(UUID.randomUUID(), "INITIAL");

        // Listener must swallow the exception and not propagate — offset will be committed
        listener.onPaymentCompleted(payload, "payment.completed.v1");

        verify(activateUseCase).execute(any(), eq("INITIAL"));
    }

    @Test
    void failedEvent_subscriptionNotFound_doesNotThrow() {
        doThrow(new SubscriptionNotFoundException(UUID.randomUUID()))
                .when(handleFailedUseCase).execute(any(), any(), any());

        String payload = buildFailedPayload(UUID.randomUUID(), "INITIAL", "card_declined");

        listener.onPaymentFailed(payload, "payment.failed.v1");

        verify(handleFailedUseCase).execute(any(), eq("INITIAL"), eq("card_declined"));
    }

    @Test
    void completedEvent_malformedJson_doesNotThrow() {
        listener.onPaymentCompleted("not valid json {{", "payment.completed.v1");

        verify(activateUseCase, never()).execute(any(), any());
    }

    @Test
    void failedEvent_malformedJson_doesNotThrow() {
        listener.onPaymentFailed("{broken", "payment.failed.v1");

        verify(handleFailedUseCase, never()).execute(any(), any(), any());
    }

    @Test
    void completedEvent_renewalType_passesCorrectType() {
        UUID subscriptionId = UUID.randomUUID();
        String payload = buildCompletedPayload(subscriptionId, "RENEWAL");

        listener.onPaymentCompleted(payload, "payment.completed.v1");

        verify(activateUseCase).execute(eq(subscriptionId), eq("RENEWAL"));
    }

    @Test
    void failedEvent_renewalType_passesCorrectType() {
        UUID subscriptionId = UUID.randomUUID();
        String payload = buildFailedPayload(subscriptionId, "RENEWAL", "timeout");

        listener.onPaymentFailed(payload, "payment.failed.v1");

        verify(handleFailedUseCase).execute(eq(subscriptionId), eq("RENEWAL"), eq("timeout"));
    }

    @Test
    void completedEvent_missingPaymentType_defaultsToInitial() {
        UUID subscriptionId = UUID.randomUUID();
        String payload = "{\"subscriptionId\":\"" + subscriptionId + "\",\"outcome\":\"completed\"}";

        listener.onPaymentCompleted(payload, "payment.completed.v1");

        verify(activateUseCase).execute(eq(subscriptionId), eq("INITIAL"));
    }

    private String buildCompletedPayload(UUID subscriptionId, String paymentType) {
        return "{\"subscriptionId\":\"" + subscriptionId + "\","
                + "\"paymentType\":\"" + paymentType + "\","
                + "\"outcome\":\"completed\"}";
    }

    private String buildFailedPayload(UUID subscriptionId, String paymentType, String reason) {
        return "{\"subscriptionId\":\"" + subscriptionId + "\","
                + "\"paymentType\":\"" + paymentType + "\","
                + "\"reason\":\"" + reason + "\","
                + "\"outcome\":\"failed\"}";
    }
}
