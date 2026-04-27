package com.mini.sardis.payment.infrastructure.adapter.out.jpa;

import com.mini.sardis.payment.application.port.out.RefundRepositoryPort;
import com.mini.sardis.payment.domain.entity.Refund;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JpaRefundRepositoryAdapter implements RefundRepositoryPort {

    private final JpaRefundRepository jpaRepo;

    @Override
    public Refund save(Refund refund) {
        return toDomain(jpaRepo.save(toJpa(refund)));
    }

    @Override
    public Optional<Refund> findBySubscriptionId(UUID subscriptionId) {
        return jpaRepo.findBySubscriptionId(subscriptionId).map(this::toDomain);
    }

    private RefundJpaEntity toJpa(Refund r) {
        return RefundJpaEntity.builder()
                .id(r.getId())
                .paymentId(r.getPaymentId())
                .subscriptionId(r.getSubscriptionId())
                .userId(r.getUserId())
                .amount(r.getAmount())
                .currency(r.getCurrency())
                .status(r.getStatus())
                .reason(r.getReason())
                .requestedAt(r.getRequestedAt())
                .processedAt(r.getProcessedAt())
                .build();
    }

    private Refund toDomain(RefundJpaEntity e) {
        return Refund.reconstruct(e.getId(), e.getPaymentId(), e.getSubscriptionId(), e.getUserId(),
                e.getAmount(), e.getCurrency(), e.getStatus(), e.getReason(),
                e.getRequestedAt(), e.getProcessedAt());
    }
}
