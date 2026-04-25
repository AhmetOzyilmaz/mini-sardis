package com.mini.sardis.application.port.out;

import com.mini.sardis.domain.entity.PromoCode;

import java.util.Optional;

public interface PromoCodeRepositoryPort {
    Optional<PromoCode> findByCode(String code);
    PromoCode save(PromoCode promoCode);
}
