package com.mini.sardis.application.service.auth;

import com.mini.sardis.application.exception.EmailAlreadyExistsException;
import com.mini.sardis.application.port.in.auth.RegisterCommand;
import com.mini.sardis.application.port.in.auth.RegisterUserUseCase;
import com.mini.sardis.application.port.out.UserRepositoryPort;
import com.mini.sardis.domain.entity.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterUserService implements RegisterUserUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegisterUserService(UserRepositoryPort userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void execute(RegisterCommand command) {
        if (userRepository.existsByEmail(command.email())) {
            throw new EmailAlreadyExistsException(command.email());
        }

        String passwordHash = passwordEncoder.encode(command.password());
        User user = User.create(command.email(), command.fullName(), command.phoneNumber(), passwordHash);
        userRepository.save(user);
    }
}
