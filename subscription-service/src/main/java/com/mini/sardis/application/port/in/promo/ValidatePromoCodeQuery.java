package com.mini.sardis.application.port.in.promo;

import com.mini.sardis.domain.value.DiscountType;

import java.math.BigDecimal;

public record ValidatePromoCodeQuery(
        String code,
        DiscountType discountType,
        BigDecimal discountValue,
        Integer maxUses,
        int currentUses,
        boolean valid,
        String message
) {}
