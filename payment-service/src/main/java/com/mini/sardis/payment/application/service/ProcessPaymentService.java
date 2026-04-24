package com.mini.sardis.payment.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mini.sardis.payment.application.port.in.ProcessPaymentCommand;
import com.mini.sardis.payment.application.port.in.ProcessPaymentUseCase;
import com.mini.sardis.payment.application.port.out.EventPublisherPort;
import com.mini.sardis.payment.application.port.out.ExternalPaymentPort;
import com.mini.sardis.payment.application.port.out.PaymentRepositoryPort;
import com.mini.sardis.payment.domain.entity.Payment;
import com.mini.sardis.payment.domain.value.PaymentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class ProcessPaymentService implements ProcessPaymentUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessPaymentService.class);
    private static final int MAX_RETRIES = 3;

    private final PaymentRepositoryPort paymentRepo;
    private final ExternalPaymentPort externalPayment;
    private final EventPublisherPort eventPublisher;
    private final ObjectMapper objectMapper;

    public ProcessPaymentService(PaymentRepositoryPort paymentRepo,
                                  ExternalPaymentPort externalPayment,
                                  EventPublisherPort eventPublisher,
                                  ObjectMapper objectMapper) {
        this.paymentRepo = paymentRepo;
        this.externalPayment = externalPayment;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void execute(ProcessPaymentCommand command) {
        // Idempotency check: skip if already processed
        if (paymentRepo.findByIdempotencyKey(command.idempotencyKey())
                .filter(p -> p.getStatus() != PaymentStatus.PENDING)
                .isPresent()) {
            log.info("Duplicate payment request skipped: {}", command.idempotencyKey());
            return;
        }

        Payment payment = Payment.create(command.subscriptionId(), command.userId(),
                command.idempotencyKey(), command.amount(), command.currency(), command.paymentType());
        payment = paymentRepo.save(payment);

        ExternalPaymentPort.ExternalPaymentResult result = chargeWithRetry(payment);

        if (result.success()) {
            payment.markSuccess(result.externalRef());
            paymentRepo.save(payment);
            log.info("Payment succeeded for subscription={} type={}", command.subscriptionId(), command.paymentType());
            eventPublisher.publish("payment.completed.v1",
                    command.subscriptionId().toString(),
                    buildEvent(payment, "completed"));
        } else {
            payment.markFailed(result.failureReason());
            paymentRepo.save(payment);
            log.warn("Payment failed for subscription={} reason={}", command.subscriptionId(), result.failureReason());
            eventPublisher.publish("payment.failed.v1",
                    command.subscriptionId().toString(),
                    buildEvent(payment, "failed"));
        }
    }

    private ExternalPaymentPort.ExternalPaymentResult chargeWithRetry(Payment payment) {
        ExternalPaymentPort.ExternalPaymentResult result = null;
        long delayMs = 1000;
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            result = externalPayment.charge(payment.getIdempotencyKey(),
                    payment.getAmount(), payment.getCurrency());
            if (result.success()) return result;
            payment.incrementRetry();
            paymentRepo.save(payment);
            if (attempt < MAX_RETRIES - 1) {
                try { Thread.sleep(delayMs); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                delayMs *= 2;
            }
        }
        return result;
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
            throw new RuntimeException("Failed to serialize payment event", e);
        }
    }
}
