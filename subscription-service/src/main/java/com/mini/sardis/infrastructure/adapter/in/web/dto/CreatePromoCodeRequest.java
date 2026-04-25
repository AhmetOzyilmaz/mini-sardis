package com.mini.sardis.infrastructure.adapter.in.web.dto;

import com.mini.sardis.domain.value.DiscountType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreatePromoCodeRequest(
        @NotBlank @Size(min = 5, max = 20) @Pattern(regexp = "^[A-Z0-9]+$") String code,
        @NotNull DiscountType discountType,
        @NotNull @DecimalMin("0.01") BigDecimal discountValue,
        @Min(1) Integer maxUses,
        LocalDateTime validFrom,
        LocalDateTime validTo
) {}
