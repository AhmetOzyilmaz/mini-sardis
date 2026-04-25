package com.mini.sardis.payment.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mini.sardis.payment.application.port.in.ProcessPaymentCommand;
import com.mini.sardis.payment.application.port.out.EventPublisherPort;
import com.mini.sardis.payment.application.port.out.ExternalPaymentPort;
import com.mini.sardis.payment.application.port.out.PaymentRepositoryPort;
import com.mini.sardis.payment.domain.entity.Payment;
import com.mini.sardis.payment.domain.value.PaymentStatus;
import com.mini.sardis.payment.domain.value.PaymentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessPaymentServiceTest {

    @Mock private PaymentRepositoryPort paymentRepo;
    @Mock private ExternalPaymentPort externalPayment;
    @Mock private EventPublisherPort eventPublisher;

    private ProcessPaymentService service;

    @BeforeEach
    void setUp() {
        service = new ProcessPaymentService(paymentRepo, externalPayment, eventPublisher, new ObjectMapper());
    }

    @Test
    void successfulPayment_publishesCompletedEvent() {
        ProcessPaymentCommand cmd = buildCommand("key-1", PaymentType.INITIAL);

        when(paymentRepo.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        when(paymentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(externalPayment.charge(any(), any(), any(), any()))
                .thenReturn(new ExternalPaymentPort.ExternalPaymentResult(true, "ext-ref-123", null));

        service.execute(cmd);

        verify(eventPublisher).publish(eq("payment.completed.v1"), anyString(), anyString());
        verify(eventPublisher, never()).publish(eq("payment.failed.v1"), anyString(), anyString());
    }

    @Test
    void failedPayment_publishesFailedEvent() {
        ProcessPaymentCommand cmd = buildCommand("key-2", PaymentType.INITIAL);

        when(paymentRepo.findByIdempotencyKey("key-2")).thenReturn(Optional.empty());
        when(paymentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(externalPayment.charge(any(), any(), any(), any()))
                .thenReturn(new ExternalPaymentPort.ExternalPaymentResult(false, null, "insufficient_funds"));

        service.execute(cmd);

        verify(eventPublisher).publish(eq("payment.failed.v1"), anyString(), anyString());
        verify(eventPublisher, never()).publish(eq("payment.completed.v1"), anyString(), anyString());
    }

    @Test
    void duplicateIdempotencyKey_skipsProcessing() {
        Payment existing = Payment.builder()
                .id(UUID.randomUUID())
                .subscriptionId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .idempotencyKey("key-dup")
                .amount(BigDecimal.valueOf(99.99))
                .currency("TRY")
                .status(PaymentStatus.SUCCESS)
                .type(PaymentType.INITIAL)
                .build();

        when(paymentRepo.findByIdempotencyKey("key-dup")).thenReturn(Optional.of(existing));

        service.execute(buildCommand("key-dup", PaymentType.INITIAL));

        verify(externalPayment, never()).charge(any(), any(), any(), any());
        verify(eventPublisher, never()).publish(any(), any(), any());
    }

    @Test
    void failedPayment_retriesThreeTimes() {
        ProcessPaymentCommand cmd = buildCommand("key-retry", PaymentType.RENEWAL);

        when(paymentRepo.findByIdempotencyKey("key-retry")).thenReturn(Optional.empty());
        when(paymentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(externalPayment.charge(any(), any(), any(), any()))
                .thenReturn(new ExternalPaymentPort.ExternalPaymentResult(false, null, "timeout"));

        service.execute(cmd);

        verify(externalPayment, times(3)).charge(any(), any(), any(), any());
        verify(eventPublisher).publish(eq("payment.failed.v1"), anyString(), anyString());
    }

    @Test
    void renewalPayment_eventIncludesPaymentType() {
        ProcessPaymentCommand cmd = buildCommand("key-renewal", PaymentType.RENEWAL);

        when(paymentRepo.findByIdempotencyKey("key-renewal")).thenReturn(Optional.empty());
        when(paymentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(externalPayment.charge(any(), any(), any(), any()))
                .thenReturn(new ExternalPaymentPort.ExternalPaymentResult(true, "ref-renewal", null));

        service.execute(cmd);

        verify(eventPublisher).publish(eq("payment.completed.v1"), anyString(),
                argThat(payload -> payload.contains("RENEWAL")));
    }

    private ProcessPaymentCommand buildCommand(String idempotencyKey, PaymentType type) {
        return new ProcessPaymentCommand(
                UUID.randomUUID(), UUID.randomUUID(),
                idempotencyKey, BigDecimal.valueOf(99.99), "TRY", type);
    }
}
