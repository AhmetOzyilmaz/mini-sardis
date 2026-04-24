package com.mini.sardis.application.port.in.auth;

public record RegisterCommand(
        String email,
        String password,
        String fullName,
        String phoneNumber
) {}
