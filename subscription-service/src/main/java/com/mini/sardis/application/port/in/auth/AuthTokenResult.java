package com.mini.sardis.application.port.in.auth;

import com.mini.sardis.domain.value.UserRole;

import java.util.UUID;

public record AuthTokenResult(
        String token,
        long expiresIn,
        UUID userId,
        UserRole role
) {}
