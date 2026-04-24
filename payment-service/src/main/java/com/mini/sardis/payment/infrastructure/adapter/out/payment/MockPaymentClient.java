package com.mini.sardis.payment.infrastructure.adapter.out.payment;

import com.mini.sardis.payment.application.port.out.ExternalPaymentPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Simulates an external payment provider.
 * Succeeds 80% of the time to allow failure scenario testing.
 */
@Component
public class MockPaymentClient implements ExternalPaymentPort {

    private static final Logger log = LoggerFactory.getLogger(MockPaymentClient.class);
    private static final double SUCCESS_RATE = 0.8;

    @Override
    public ExternalPaymentResult charge(String idempotencyKey, BigDecimal amount, String currency) {
        log.info("MockPaymentClient: charging {} {} key={}", amount, currency, idempotencyKey);

        // Simulate network delay
        try { Thread.sleep(ThreadLocalRandom.current().nextInt(100, 400)); }
        catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }

        boolean success = ThreadLocalRandom.current().nextDouble() < SUCCESS_RATE;
        if (success) {
            String externalRef = "mock-ref-" + UUID.randomUUID();
            log.info("MockPaymentClient: SUCCESS ref={}", externalRef);
            return new ExternalPaymentResult(true, externalRef, null);
        } else {
            log.warn("MockPaymentClient: FAILED insufficient_funds");
            return new ExternalPaymentResult(false, null, "insufficient_funds");
        }
    }
}
