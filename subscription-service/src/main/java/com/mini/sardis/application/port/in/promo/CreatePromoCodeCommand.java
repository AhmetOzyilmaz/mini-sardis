package com.mini.sardis.application.port.in.promo;

import com.mini.sardis.domain.value.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

public record CreatePromoCodeCommand(
        String code,
        DiscountType discountType,
        BigDecimal discountValue,
        Integer maxUses,
        LocalDateTime validFrom,
        LocalDateTime validTo,
        Set<Integer> applicableMonths
) {
    public CreatePromoCodeCommand(String code, DiscountType discountType, BigDecimal discountValue,
                                   Integer maxUses, LocalDateTime validFrom, LocalDateTime validTo) {
        this(code, discountType, discountValue, maxUses, validFrom, validTo, null);
    }
}
