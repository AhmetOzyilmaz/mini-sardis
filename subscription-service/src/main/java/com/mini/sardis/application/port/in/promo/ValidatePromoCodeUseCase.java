package com.mini.sardis.application.port.in.promo;

public interface ValidatePromoCodeUseCase {
    ValidatePromoCodeQuery validate(String code, Integer durationMonths);

    default ValidatePromoCodeQuery validate(String code) {
        return validate(code, null);
    }
}
