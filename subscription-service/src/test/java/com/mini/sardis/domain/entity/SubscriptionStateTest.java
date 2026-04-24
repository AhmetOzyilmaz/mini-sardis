package com.mini.sardis.domain.entity;

import com.mini.sardis.domain.value.SubscriptionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class SubscriptionStateTest {

    private Subscription subscription;

    @BeforeEach
    void setUp() {
        subscription = Subscription.create(UUID.randomUUID(), UUID.randomUUID(),
                BigDecimal.valueOf(99.99), "TRY");
    }

    @Test
    void newSubscriptionIsPending() {
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.PENDING);
    }

    @Test
    void pendingActivatesToActive() {
        subscription.activate(30);

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.getStartDate()).isEqualTo(LocalDate.now());
        assertThat(subscription.getNextRenewalDate()).isEqualTo(LocalDate.now().plusDays(30));
    }

    @Test
    void suspendedCanBeActivated() {
        subscription.activate(30);
        subscription.suspend();

        subscription.activate(30);

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    void activateFromCancelledThrows() {
        subscription.cancel("user_request");

        assertThatThrownBy(() -> subscription.activate(30))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void activeCanBeCancelled() {
        subscription.activate(30);

        subscription.cancel("user_request");

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        assertThat(subscription.getCancelledAt()).isNotNull();
        assertThat(subscription.getCancellationReason()).isEqualTo("user_request");
    }

    @Test
    void pendingCanBeCancelled() {
        subscription.cancel("payment_failed");

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
    }

    @Test
    void cancelledCannotBeCancelledAgain() {
        subscription.cancel("user_request");

        assertThatThrownBy(() -> subscription.cancel("user_request"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already cancelled");
    }

    @Test
    void activeCanBeSuspended() {
        subscription.activate(30);

        subscription.suspend();

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.SUSPENDED);
    }

    @Test
    void pendingCannotBeSuspended() {
        assertThatThrownBy(() -> subscription.suspend())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cancelledCannotBeSuspended() {
        subscription.cancel("reason");

        assertThatThrownBy(() -> subscription.suspend())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void extendRenewalAddsToCurrentNextRenewalDate() {
        subscription.activate(30);
        LocalDate originalRenewal = subscription.getNextRenewalDate();

        subscription.extendRenewal(30);

        assertThat(subscription.getNextRenewalDate()).isEqualTo(originalRenewal.plusDays(30));
    }

    @Test
    void isEligibleForRenewalWhenActiveDue() {
        Subscription past = Subscription.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .planId(UUID.randomUUID())
                .status(SubscriptionStatus.ACTIVE)
                .nextRenewalDate(LocalDate.now().minusDays(1))
                .amount(BigDecimal.valueOf(99.99))
                .currency("TRY")
                .version(0)
                .build();

        assertThat(past.isEligibleForRenewal()).isTrue();
    }

    @Test
    void isNotEligibleForRenewalWhenFutureDate() {
        subscription.activate(30);

        assertThat(subscription.isEligibleForRenewal()).isFalse();
    }

    @Test
    void isNotEligibleForRenewalWhenNotActive() {
        assertThat(subscription.isEligibleForRenewal()).isFalse();
    }
}
