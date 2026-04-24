package com.mini.sardis.application.port.in.security;

import com.mini.sardis.domain.value.UserRole;

import java.util.UUID;

public interface AuthenticationPort {

    UUID currentUserId();

    UserRole currentUserRole();

    boolean isAdmin();
}
