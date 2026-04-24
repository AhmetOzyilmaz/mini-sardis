package com.mini.sardis.infrastructure.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(

        @NotBlank(message = "JWT secret must not be blank — set APP_JWT_SECRET env var")
        String secret,

        @Min(value = 3600000, message = "JWT expiration must be at least 1 hour")
        long expirationMs
) {}
