package com.mini.sardis.application.port.in.promo;

import com.mini.sardis.domain.value.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreatePromoCodeCommand(
        String code,
        DiscountType discountType,
        BigDecimal discountValue,
        Integer maxUses,
        LocalDateTime validFrom,
        LocalDateTime validTo
) {}
