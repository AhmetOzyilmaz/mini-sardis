package com.mini.sardis.domain.value;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.Objects;

@Getter
public final class Money {

    private final BigDecimal amount;
    private final String currency;

    private Money(BigDecimal amount, String currency) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Amount must be non-negative");
        this.amount = amount;
        this.currency = Objects.requireNonNull(currency, "Currency required");
    }

    public static Money of(BigDecimal amount, String currency) {
        return new Money(amount, currency);
    }

    public static Money ofTRY(BigDecimal amount) {
        return new Money(amount, "TRY");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money m)) return false;
        return amount.compareTo(m.amount) == 0 && currency.equals(m.currency);
    }

    @Override
    public int hashCode() { return Objects.hash(amount.stripTrailingZeros(), currency); }

    @Override
    public String toString() { return amount.toPlainString() + " " + currency; }
}
