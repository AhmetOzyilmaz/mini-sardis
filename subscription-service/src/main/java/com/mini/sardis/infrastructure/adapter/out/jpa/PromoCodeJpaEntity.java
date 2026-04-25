package com.mini.sardis.infrastructure.adapter.out.jpa;

import com.mini.sardis.domain.value.DiscountType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "promo_codes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PromoCodeJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 20, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "max_uses")
    private Integer maxUses;

    @Column(name = "current_uses", nullable = false)
    private int currentUses;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    @Column(name = "valid_to")
    private LocalDateTime validTo;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
