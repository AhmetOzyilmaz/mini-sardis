package com.mini.sardis.infrastructure.adapter.in.web.dto;

import com.mini.sardis.domain.entity.PromoCode;
import com.mini.sardis.domain.value.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PromoCodeResponse(
        UUID id,
        String code,
        DiscountType discountType,
        BigDecimal discountValue,
        Integer maxUses,
        int currentUses,
        boolean active,
        LocalDateTime validFrom,
        LocalDateTime validTo
) {
    public static PromoCodeResponse from(PromoCode p) {
        return new PromoCodeResponse(
                p.getId(), p.getCode(), p.getDiscountType(), p.getDiscountValue(),
                p.getMaxUses(), p.getCurrentUses(), p.isActive(),
                p.getValidFrom(), p.getValidTo());
    }
}
