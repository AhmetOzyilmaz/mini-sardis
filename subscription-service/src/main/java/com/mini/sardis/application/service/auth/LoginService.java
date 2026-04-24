package com.mini.sardis.application.service.auth;

import com.mini.sardis.application.exception.InvalidCredentialsException;
import com.mini.sardis.application.port.in.auth.AuthTokenResult;
import com.mini.sardis.application.port.in.auth.LoginCommand;
import com.mini.sardis.application.port.in.auth.LoginUseCase;
import com.mini.sardis.application.port.out.TokenGeneratorPort;
import com.mini.sardis.application.port.out.UserRepositoryPort;
import com.mini.sardis.domain.entity.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class LoginService implements LoginUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenGeneratorPort tokenGenerator;

    public LoginService(UserRepositoryPort userRepository,
                        PasswordEncoder passwordEncoder,
                        TokenGeneratorPort tokenGenerator) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenGenerator = tokenGenerator;
    }

    @Override
    public AuthTokenResult execute(LoginCommand command) {
        User user = userRepository.findByEmail(command.email())
                .filter(User::isActive)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(command.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String token = tokenGenerator.generateToken(user);
        long expiresIn = tokenGenerator.getExpirationMs() / 1000;

        return new AuthTokenResult(token, expiresIn, user.getId(), user.getRole());
    }
}
