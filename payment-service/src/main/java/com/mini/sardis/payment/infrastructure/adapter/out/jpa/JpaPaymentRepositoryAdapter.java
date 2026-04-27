package com.mini.sardis.payment.infrastructure.adapter.out.jpa;

import com.mini.sardis.payment.application.port.out.PaymentRepositoryPort;
import com.mini.sardis.payment.domain.entity.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JpaPaymentRepositoryAdapter implements PaymentRepositoryPort {

    private final JpaPaymentRepository jpaRepo;

    @Override
    public Payment save(Payment payment) {
        return toDomain(jpaRepo.save(toJpa(payment)));
    }

    @Override
    public Optional<Payment> findByIdempotencyKey(String key) {
        return jpaRepo.findByIdempotencyKey(key).map(this::toDomain);
    }

    @Override
    public List<Payment> findBySubscriptionId(UUID subscriptionId) {
        return jpaRepo.findBySubscriptionId(subscriptionId).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Payment> findLatestSuccessBySubscriptionId(UUID subscriptionId) {
        return jpaRepo.findLatestSuccessBySubscriptionId(subscriptionId).map(this::toDomain);
    }

    private PaymentJpaEntity toJpa(Payment p) {
        return PaymentJpaEntity.builder()
                .id(p.getId())
                .subscriptionId(p.getSubscriptionId())
                .userId(p.getUserId())
                .idempotencyKey(p.getIdempotencyKey())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .status(p.getStatus())
                .type(p.getType())
                .paymentMethod(p.getPaymentMethod())
                .externalRef(p.getExternalRef())
                .failureReason(p.getFailureReason())
                .retryCount(p.getRetryCount())
                .processedAt(p.getProcessedAt())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private Payment toDomain(PaymentJpaEntity e) {
        return Payment.builder()
                .id(e.getId())
                .subscriptionId(e.getSubscriptionId())
                .userId(e.getUserId())
                .idempotencyKey(e.getIdempotencyKey())
                .amount(e.getAmount())
                .currency(e.getCurrency())
                .status(e.getStatus())
                .type(e.getType())
                .paymentMethod(e.getPaymentMethod())
                .externalRef(e.getExternalRef())
                .failureReason(e.getFailureReason())
                .retryCount(e.getRetryCount())
                .processedAt(e.getProcessedAt())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
