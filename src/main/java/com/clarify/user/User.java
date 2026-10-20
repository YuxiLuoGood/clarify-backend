package com.clarify.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    /**
     * Stored as the password of accounts created through Google sign-in. It is
     * not a bcrypt hash, so password login for those accounts always fails
     * (as it does for the "GOOGLE_OAUTH_..." values the old version wrote).
     */
    public static final String OAUTH_PASSWORD_PLACEHOLDER = "!oauth:google";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    protected User() {
        // for JPA
    }

    private User(String email, String passwordHash, String name, LocalDateTime createdAt) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
        this.createdAt = createdAt;
    }

    public static User withPassword(String email, String passwordHash, String name, LocalDateTime now) {
        return new User(email, passwordHash, name, now);
    }

    public static User fromGoogle(String email, String name, LocalDateTime now) {
        return new User(email, OAUTH_PASSWORD_PLACEHOLDER, name == null || name.isBlank() ? email : name, now);
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getName() { return name; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
