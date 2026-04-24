package com.mini.sardis.application.port.in.auth;

public interface LoginUseCase {

    AuthTokenResult execute(LoginCommand command);
}
