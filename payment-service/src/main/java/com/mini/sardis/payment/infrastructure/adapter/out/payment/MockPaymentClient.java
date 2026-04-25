package com.mini.sardis.payment.infrastructure.adapter.out.payment;

import com.mini.sardis.payment.application.port.out.ExternalPaymentPort;
import com.mini.sardis.payment.domain.value.PaymentMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Simulates external payment providers per payment method.
 * Each method has a distinct success rate to allow realistic scenario testing.
 */
@Slf4j
@Component
public class MockPaymentClient implements ExternalPaymentPort {

    @Override
    public ExternalPaymentResult charge(String idempotencyKey, BigDecimal amount, String currency,
                                         PaymentMethod paymentMethod) {
        log.info("MockPaymentClient [{}]: charging {} {} key={}", paymentMethod, amount, currency, idempotencyKey);

        simulateNetworkDelay(paymentMethod);

        double successRate = successRateFor(paymentMethod);
        boolean success = ThreadLocalRandom.current().nextDouble() < successRate;

        if (success) {
            String externalRef = paymentMethod.name().toLowerCase() + "-ref-" + UUID.randomUUID();
            log.info("MockPaymentClient [{}]: SUCCESS ref={}", paymentMethod, externalRef);
            return new ExternalPaymentResult(true, externalRef, null);
        } else {
            String reason = failureReasonFor(paymentMethod);
            log.warn("MockPaymentClient [{}]: FAILED reason={}", paymentMethod, reason);
            return new ExternalPaymentResult(false, null, reason);
        }
    }

    private double successRateFor(PaymentMethod method) {
        return switch (method) {
            case CREDIT_CARD    -> 0.85;
            case DEBIT_CARD     -> 0.80;
            case BANK_TRANSFER  -> 0.95;
            case DIGITAL_WALLET -> 0.90;
        };
    }

    private String failureReasonFor(PaymentMethod method) {
        return switch (method) {
            case CREDIT_CARD    -> "insufficient_funds";
            case DEBIT_CARD     -> "daily_limit_exceeded";
            case BANK_TRANSFER  -> "bank_connection_timeout";
            case DIGITAL_WALLET -> "wallet_balance_insufficient";
        };
    }

    private void simulateNetworkDelay(PaymentMethod method) {
        int minMs = switch (method) {
            case CREDIT_CARD    -> 100;
            case DEBIT_CARD     -> 100;
            case BANK_TRANSFER  -> 300;
            case DIGITAL_WALLET -> 50;
        };
        int maxMs = minMs + 200;
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(minMs, maxMs));
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
