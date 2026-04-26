package com.mini.sardis.infrastructure.adapter.out.jpa;

import com.mini.sardis.domain.value.OfferTargetType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "offers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OfferJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Column(name = "promo_code_id")
    private UUID promoCodeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    private OfferTargetType targetType;

    @Column(name = "target_user_id")
    private UUID targetUserId;

    @Column(name = "target_plan_id")
    private UUID targetPlanId;

    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    @Column(name = "valid_to")
    private LocalDateTime validTo;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
