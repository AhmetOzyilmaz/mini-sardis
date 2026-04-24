package com.mini.sardis.domain.entity;

import com.mini.sardis.domain.value.UserRole;

import java.time.LocalDateTime;
import java.util.UUID;

public class User {

    private final UUID id;
    private final String email;
    private final String fullName;
    private final String phoneNumber;
    private final String passwordHash;
    private final UserRole role;
    private final boolean active;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private User(Builder builder) {
        this.id = builder.id;
        this.email = builder.email;
        this.fullName = builder.fullName;
        this.phoneNumber = builder.phoneNumber;
        this.passwordHash = builder.passwordHash;
        this.role = builder.role;
        this.active = builder.active;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
    }

    public static User create(String email, String fullName, String phoneNumber, String passwordHash) {
        return new Builder()
                .id(UUID.randomUUID())
                .email(email)
                .fullName(fullName)
                .phoneNumber(phoneNumber)
                .passwordHash(passwordHash)
                .role(UserRole.USER)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getPasswordHash() { return passwordHash; }
    public UserRole getRole() { return role; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private String email;
        private String fullName;
        private String phoneNumber;
        private String passwordHash;
        private UserRole role = UserRole.USER;
        private boolean active = true;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder fullName(String fullName) { this.fullName = fullName; return this; }
        public Builder phoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; return this; }
        public Builder passwordHash(String passwordHash) { this.passwordHash = passwordHash; return this; }
        public Builder role(UserRole role) { this.role = role; return this; }
        public Builder active(boolean active) { this.active = active; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public User build() { return new User(this); }
    }
}
