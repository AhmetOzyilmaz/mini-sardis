package com.mini.sardis.application.port.in.promo;

import java.util.List;
import java.util.UUID;

public record AssignPromoCodeCommand(String code, List<UUID> userIds) {}
