package com.mini.sardis.payment.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mini.sardis.payment.application.port.in.HandleWebhookCommand;
import com.mini.sardis.payment.application.port.out.EventPublisherPort;
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
class HandleWebhookServiceTest {

    @Mock private PaymentRepositoryPort paymentRepo;
    @Mock private EventPublisherPort eventPublisher;

    private HandleWebhookService service;

    @BeforeEach
    void setUp() {
        service = new HandleWebhookService(paymentRepo, eventPublisher, new ObjectMapper());
    }

    @Test
    void successWebhook_marksSuccessAndPublishesCompletedEvent() {
        Payment payment = pendingPayment("key-success");
        when(paymentRepo.findByIdempotencyKey("key-success")).thenReturn(Optional.of(payment));
        when(paymentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.execute(new HandleWebhookCommand("key-success", "ext-ref", true, null));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getExternalRef()).isEqualTo("ext-ref");
        verify(eventPublisher).publish(eq("payment.completed.v1"), anyString(), anyString());
    }

    @Test
    void failureWebhook_marksFailedAndPublishesFailedEvent() {
        Payment payment = pendingPayment("key-fail");
        when(paymentRepo.findByIdempotencyKey("key-fail")).thenReturn(Optional.of(payment));
        when(paymentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.execute(new HandleWebhookCommand("key-fail", null, false, "card_declined"));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getFailureReason()).isEqualTo("card_declined");
        verify(eventPublisher).publish(eq("payment.failed.v1"), anyString(), anyString());
    }

    @Test
    void duplicateWebhook_alreadyProcessed_isSkipped() {
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .subscriptionId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .idempotencyKey("key-dup")
                .amount(BigDecimal.valueOf(99.99))
                .currency("TRY")
                .status(PaymentStatus.SUCCESS)
                .type(PaymentType.INITIAL)
                .build();

        when(paymentRepo.findByIdempotencyKey("key-dup")).thenReturn(Optional.of(payment));

        service.execute(new HandleWebhookCommand("key-dup", "ext-ref", true, null));

        verify(paymentRepo, never()).save(any());
        verify(eventPublisher, never()).publish(any(), any(), any());
    }

    @Test
    void webhook_throwsWhenPaymentNotFound() {
        when(paymentRepo.findByIdempotencyKey("key-missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(
                new HandleWebhookCommand("key-missing", null, true, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Payment pendingPayment(String idempotencyKey) {
        return Payment.builder()
                .id(UUID.randomUUID())
                .subscriptionId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .idempotencyKey(idempotencyKey)
                .amount(BigDecimal.valueOf(99.99))
                .currency("TRY")
                .status(PaymentStatus.PENDING)
                .type(PaymentType.INITIAL)
                .build();
    }
}
