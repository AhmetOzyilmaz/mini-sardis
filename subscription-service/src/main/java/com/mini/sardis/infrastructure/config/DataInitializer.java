package com.mini.sardis.infrastructure.config;

import com.mini.sardis.application.port.out.UserRepositoryPort;
import com.mini.sardis.domain.entity.User;
import com.mini.sardis.domain.value.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@Profile({"dev", "default"})
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepositoryPort userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepositoryPort userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmail("admin@demo.com")) {
            log.info("Demo seed data already present — skipping");
            return;
        }

        String demoHash = passwordEncoder.encode("Password1");

        // Admin user (ROLE_ADMIN) — password: Admin1234
        userRepository.save(User.builder()
                .id(UUID.fromString("b0000001-0000-0000-0000-000000000001"))
                .email("admin@demo.com")
                .fullName("Admin User")
                .phoneNumber("+905001234567")
                .passwordHash(passwordEncoder.encode("Admin1234"))
                .role(UserRole.ADMIN)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build());

        // Regular users (ROLE_USER) — password: Password1
        userRepository.save(User.create("user1@demo.com", "Alice Demo",   "+905001234568", demoHash));
        userRepository.save(User.create("user2@demo.com", "Bob Demo",     "+905001234569", demoHash));
        userRepository.save(User.create("user3@demo.com", "Charlie Demo", null,            demoHash));

        log.info("Demo seed data loaded — admin@demo.com / Admin1234, user1-3@demo.com / Password1");
    }
}
