package com.mini.sardis.application.port.in.auth;

public record LoginCommand(
        String email,
        String password
) {}
