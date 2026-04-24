package com.mini.sardis.application.port.out;

import com.mini.sardis.domain.entity.User;

public interface TokenGeneratorPort {
    String generateToken(User user);
    long getExpirationMs();
}
