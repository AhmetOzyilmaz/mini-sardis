package com.mini.sardis.infrastructure.adapter.in.web;

import com.mini.sardis.application.port.in.auth.AuthTokenResult;
import com.mini.sardis.application.port.in.auth.LoginCommand;
import com.mini.sardis.application.port.in.auth.LoginUseCase;
import com.mini.sardis.application.port.in.auth.RegisterCommand;
import com.mini.sardis.application.port.in.auth.RegisterUserUseCase;
import com.mini.sardis.infrastructure.adapter.in.web.dto.AuthResponse;
import com.mini.sardis.infrastructure.adapter.in.web.dto.LoginRequest;
import com.mini.sardis.infrastructure.adapter.in.web.dto.RegisterRequest;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Authentication", description = "User registration and login")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUseCase loginUseCase;
    private final MeterRegistry meterRegistry;

    @Operation(summary = "Register a new user")
    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        try {
            registerUserUseCase.execute(new RegisterCommand(
                    request.email(),
                    request.password(),
                    request.fullName(),
                    request.phoneNumber()
            ));
            counter("auth.register.attempts", "success").increment();
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (RuntimeException e) {
            counter("auth.register.attempts", "failure").increment();
            throw e;
        }
    }

    @Operation(summary = "Authenticate and receive a JWT token")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            AuthTokenResult result = loginUseCase.execute(new LoginCommand(
                    request.email(),
                    request.password()
            ));
            sample.stop(timer("auth.login.duration", "success"));
            counter("auth.login.attempts", "success").increment();
            return ResponseEntity.ok(AuthResponse.of(
                    result.token(),
                    result.expiresIn(),
                    result.userId(),
                    result.role()
            ));
        } catch (RuntimeException e) {
            sample.stop(timer("auth.login.duration", "failure"));
            counter("auth.login.attempts", "failure").increment();
            throw e;
        }
    }

    private Counter counter(String name, String result) {
        return Counter.builder(name)
                .tag("result", result)
                .register(meterRegistry);
    }

    private Timer timer(String name, String result) {
        return Timer.builder(name)
                .tag("result", result)
                .register(meterRegistry);
    }
}
