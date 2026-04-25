package com.mini.sardis.application.port.in.promo;

public interface ValidatePromoCodeUseCase {
    ValidatePromoCodeQuery validate(String code);
}
