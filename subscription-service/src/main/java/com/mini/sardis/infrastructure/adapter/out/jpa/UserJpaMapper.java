package com.mini.sardis.infrastructure.adapter.out.jpa;

import com.mini.sardis.domain.entity.User;
import org.springframework.stereotype.Component;

@Component
class UserJpaMapper {

    User toDomain(UserJpaEntity entity) {
        return User.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .fullName(entity.getFullName())
                .phoneNumber(entity.getPhoneNumber())
                .passwordHash(entity.getPasswordHash())
                .role(entity.getRole())
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    UserJpaEntity toEntity(User domain) {
        return UserJpaEntity.builder()
                .id(domain.getId())
                .email(domain.getEmail())
                .fullName(domain.getFullName())
                .phoneNumber(domain.getPhoneNumber())
                .passwordHash(domain.getPasswordHash())
                .role(domain.getRole())
                .active(domain.isActive())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
