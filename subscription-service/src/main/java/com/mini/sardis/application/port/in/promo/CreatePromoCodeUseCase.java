package com.mini.sardis.application.port.in.promo;

import com.mini.sardis.domain.entity.PromoCode;

public interface CreatePromoCodeUseCase {
    PromoCode execute(CreatePromoCodeCommand command);
}
