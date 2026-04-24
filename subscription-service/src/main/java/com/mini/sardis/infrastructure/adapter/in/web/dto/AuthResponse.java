package com.mini.sardis.infrastructure.adapter.in.web.dto;

import com.mini.sardis.domain.value.UserRole;

import java.util.UUID;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresIn,
        UUID userId,
        UserRole role
) {
    public static AuthResponse of(String token, long expiresIn, UUID userId, UserRole role) {
        return new AuthResponse(token, "Bearer", expiresIn, userId, role);
    }
}
