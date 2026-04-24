package com.mini.sardis.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateSubscriptionRequest(@NotNull UUID planId) {}
