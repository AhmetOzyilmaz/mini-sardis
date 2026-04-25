package com.mini.sardis.application.port.in.promo;

import com.mini.sardis.domain.value.DiscountType;

import java.math.BigDecimal;
import java.util.Set;

public record ValidatePromoCodeQuery(
        String code,
        DiscountType discountType,
        BigDecimal discountValue,
        Integer maxUses,
        int currentUses,
        boolean valid,
        String message,
        Set<Integer> applicableMonths
) {
    public ValidatePromoCodeQuery(String code, DiscountType discountType, BigDecimal discountValue,
                                   Integer maxUses, int currentUses, boolean valid, String message) {
        this(code, discountType, discountValue, maxUses, currentUses, valid, message, null);
    }
}
