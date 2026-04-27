package com.mini.sardis.payment.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mini.sardis.payment.application.port.in.ProcessRefundCommand;
import com.mini.sardis.payment.application.port.in.ProcessRefundUseCase;
import com.mini.sardis.payment.application.port.out.EventPublisherPort;
import com.mini.sardis.payment.application.port.out.PaymentRepositoryPort;
import com.mini.sardis.payment.application.port.out.RefundRepositoryPort;
import com.mini.sardis.payment.domain.entity.Payment;
import com.mini.sardis.payment.domain.entity.Refund;
import com.mini.sardis.payment.domain.value.RefundStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessRefundService implements ProcessRefundUseCase {

    private final PaymentRepositoryPort paymentRepo;
    private final RefundRepositoryPort refundRepo;
    private final EventPublisherPort eventPublisher;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void execute(ProcessRefundCommand command) {
        if (refundRepo.findBySubscriptionId(command.subscriptionId())
                .filter(r -> r.getStatus() != RefundStatus.REJECTED)
                .isPresent()) {
            log.info("Duplicate refund request skipped for subscriptionId={}", command.subscriptionId());
            return;
        }

        Payment payment = paymentRepo.findLatestSuccessBySubscriptionId(command.subscriptionId())
                .orElse(null);

        if (payment == null) {
            log.warn("No successful payment found for subscriptionId={}", command.subscriptionId());
            eventPublisher.publish("refund.failed.v1", command.subscriptionId().toString(),
                    buildFailedEvent(command, "no_successful_payment"));
            return;
        }

        Refund refund = Refund.create(payment.getId(), command.subscriptionId(), command.userId(),
                payment.getAmount(), payment.getCurrency(), command.reason());
        refund = refundRepo.save(refund);

        refund.markCompleted();
        refundRepo.save(refund);

        log.info("Refund completed for subscriptionId={} amount={}", command.subscriptionId(), payment.getAmount());
        eventPublisher.publish("refund.completed.v1", command.subscriptionId().toString(),
                buildCompletedEvent(refund, payment));
    }

    private String buildCompletedEvent(Refund r, Payment p) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("refundId", r.getId().toString());
            payload.put("subscriptionId", r.getSubscriptionId().toString());
            payload.put("userId", r.getUserId().toString());
            payload.put("paymentId", p.getId().toString());
            payload.put("amount", r.getAmount());
            payload.put("currency", r.getCurrency());
            payload.put("processedAt", LocalDateTime.now().toString());
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize refund.completed.v1", e);
        }
    }

    private String buildFailedEvent(ProcessRefundCommand command, String reason) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "subscriptionId", command.subscriptionId().toString(),
                    "userId", command.userId().toString(),
                    "reason", reason
            ));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize refund.failed.v1", e);
        }
    }
}
