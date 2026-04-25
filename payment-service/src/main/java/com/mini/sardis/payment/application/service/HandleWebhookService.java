package com.mini.sardis.payment.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mini.sardis.payment.application.port.in.HandleWebhookCommand;
import com.mini.sardis.payment.application.port.in.HandleWebhookUseCase;
import com.mini.sardis.payment.application.port.out.EventPublisherPort;
import com.mini.sardis.payment.application.port.out.PaymentRepositoryPort;
import com.mini.sardis.payment.domain.entity.Payment;
import com.mini.sardis.payment.domain.value.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class HandleWebhookService implements HandleWebhookUseCase {

    private final PaymentRepositoryPort paymentRepo;
    private final EventPublisherPort eventPublisher;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void execute(HandleWebhookCommand command) {
        Payment payment = paymentRepo.findByIdempotencyKey(command.idempotencyKey())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Payment not found for idempotencyKey: " + command.idempotencyKey()));

        // Idempotency: already processed
        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.info("Webhook already processed for key={}", command.idempotencyKey());
            return;
        }

        if (command.success()) {
            payment.markSuccess(command.externalRef());
            paymentRepo.save(payment);
            log.info("Webhook: payment succeeded subscription={}", payment.getSubscriptionId());
            eventPublisher.publish("payment.completed.v1",
                    payment.getSubscriptionId().toString(), buildEvent(payment, "completed"));
        } else {
            payment.markFailed(command.failureReason());
            paymentRepo.save(payment);
            log.warn("Webhook: payment failed subscription={} reason={}",
                    payment.getSubscriptionId(), command.failureReason());
            eventPublisher.publish("payment.failed.v1",
                    payment.getSubscriptionId().toString(), buildEvent(payment, "failed"));
        }
    }

    private String buildEvent(Payment p, String outcome) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "paymentId", p.getId().toString(),
                    "subscriptionId", p.getSubscriptionId().toString(),
                    "userId", p.getUserId().toString(),
                    "paymentType", p.getType().name(),
                    "amount", p.getAmount(),
                    "currency", p.getCurrency(),
                    "outcome", outcome,
                    "reason", p.getFailureReason() != null ? p.getFailureReason() : ""
            ));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize webhook event", e);
        }
    }
}
