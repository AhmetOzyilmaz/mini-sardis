package com.mini.sardis.application.port.out;

import com.mini.sardis.domain.entity.UserPromoCode;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserPromoCodeRepositoryPort {
    UserPromoCode save(UserPromoCode userPromoCode);
    List<UserPromoCode> saveAll(List<UserPromoCode> assignments);
    List<UserPromoCode> findByUserId(UUID userId);
    Optional<UserPromoCode> findByUserIdAndPromoCodeId(UUID userId, UUID promoCodeId);
    Optional<UserPromoCode> findByUserIdAndCode(UUID userId, String code);
}
