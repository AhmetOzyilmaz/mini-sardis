package com.mini.sardis.domain.entity;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class UserPromoCode {

    private final UUID id;
    private final UUID userId;
    private final UUID promoCodeId;
    private final String code;
    private final LocalDateTime assignedAt;
    private boolean used;
    private LocalDateTime usedAt;

    private UserPromoCode(UUID id, UUID userId, UUID promoCodeId, String code,
                          LocalDateTime assignedAt, boolean used, LocalDateTime usedAt) {
        this.id = id;
        this.userId = userId;
        this.promoCodeId = promoCodeId;
        this.code = code;
        this.assignedAt = assignedAt;
        this.used = used;
        this.usedAt = usedAt;
    }

    public static UserPromoCode assign(UUID userId, UUID promoCodeId, String code) {
        return new UserPromoCode(UUID.randomUUID(), userId, promoCodeId, code,
                LocalDateTime.now(), false, null);
    }

    public static UserPromoCode reconstruct(UUID id, UUID userId, UUID promoCodeId, String code,
                                            LocalDateTime assignedAt, boolean used, LocalDateTime usedAt) {
        return new UserPromoCode(id, userId, promoCodeId, code, assignedAt, used, usedAt);
    }

    public void markUsed() {
        if (used) {
            throw new IllegalStateException("Promo code already used by this assignment");
        }
        this.used = true;
        this.usedAt = LocalDateTime.now();
    }
}
