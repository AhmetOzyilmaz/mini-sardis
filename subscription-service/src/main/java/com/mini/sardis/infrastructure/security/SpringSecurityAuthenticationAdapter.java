package com.mini.sardis.infrastructure.security;

import com.mini.sardis.application.port.in.security.AuthenticationPort;
import com.mini.sardis.domain.value.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SpringSecurityAuthenticationAdapter implements AuthenticationPort {

    @Override
    public UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new IllegalStateException("No authenticated user in security context");
        }
        return (UUID) auth.getPrincipal();
    }

    @Override
    public UserRole currentUserRole() {
        return isAdmin() ? UserRole.ADMIN : UserRole.USER;
    }

    @Override
    public boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
