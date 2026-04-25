package com.mini.sardis.infrastructure.adapter.in.web.dto;

import com.mini.sardis.application.port.in.promo.ValidatePromoCodeQuery;
import com.mini.sardis.domain.value.DiscountType;

import java.math.BigDecimal;

public record ValidatePromoCodeResponse(
        String code,
        DiscountType discountType,
        BigDecimal discountValue,
        Integer maxUses,
        int currentUses,
        boolean valid,
        String message
) {
    public static ValidatePromoCodeResponse from(ValidatePromoCodeQuery q) {
        return new ValidatePromoCodeResponse(
                q.code(), q.discountType(), q.discountValue(),
                q.maxUses(), q.currentUses(), q.valid(), q.message());
    }
}
