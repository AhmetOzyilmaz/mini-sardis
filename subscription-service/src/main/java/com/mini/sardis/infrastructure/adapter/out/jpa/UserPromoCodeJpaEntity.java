package com.mini.sardis.infrastructure.adapter.out.jpa;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_promo_codes",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_upc_user_promo",
                columnNames = {"user_id", "promo_code_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserPromoCodeJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "promo_code_id", nullable = false)
    private UUID promoCodeId;

    @Column(nullable = false, length = 20)
    private String code;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    @Column(nullable = false)
    private boolean used;

    @Column(name = "used_at")
    private LocalDateTime usedAt;
}
