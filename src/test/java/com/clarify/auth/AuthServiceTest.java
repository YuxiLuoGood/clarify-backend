package com.clarify.auth;

import com.clarify.common.error.BadRequestException;
import com.clarify.security.JwtProperties;
import com.clarify.security.JwtService;
import com.clarify.user.User;
import com.clarify.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private final Clock clock = Clock.systemUTC();
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4); // fast for tests
    private final JwtService jwt = new JwtService(
            new JwtProperties("test-secret-key-that-is-definitely-long-enough", Duration.ofHours(1)), clock);

    private UserRepository users;
    private GoogleTokenVerifier google;
    private AuthService service;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        google = mock(GoogleTokenVerifier.class);
        service = new AuthService(users, encoder, jwt, google, clock);
    }

    private User existingUser(String rawPassword) {
        return User.withPassword("test@example.com", encoder.encode(rawPassword), "Test", LocalDateTime.now(clock));
    }

    @Test
    void registerStoresAHashAndReturnsAToken() {
        when(users.existsByEmail("new@example.com")).thenReturn(false);
        when(users.saveAndFlush(any(User.class))).then(returnsFirstArg());

        String token = service.register(new RegisterRequest("new@example.com", "password123", "New"));

        assertThat(jwt.verify(token)).contains("new@example.com");
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getPasswordHash()).isNotEqualTo("password123");
        assertThat(encoder.matches("password123", saved.getValue().getPasswordHash())).isTrue();
    }

    @Test
    void registerRejectsATakenEmail() {
        when(users.existsByEmail("new@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(new RegisterRequest("new@example.com", "password123", "New")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(AuthService.EMAIL_TAKEN);
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void loginWithCorrectPassword() {
        when(users.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser("password123")));

        assertThat(jwt.verify(service.login(new LoginRequest("test@example.com", "password123"))))
                .contains("test@example.com");
    }

    @Test
    void loginWithUnknownEmail() {
        when(users.findByEmail("test@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest("test@example.com", "password123")))
                .hasMessage(AuthService.USER_NOT_FOUND);
    }

    @Test
    void loginWithWrongPassword() {
        when(users.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser("password123")));

        assertThatThrownBy(() -> service.login(new LoginRequest("test@example.com", "wrong")))
                .hasMessage(AuthService.INVALID_PASSWORD);
    }

    @Test
    void googleAccountsCannotUsePasswordLogin() {
        User googleUser = User.fromGoogle("test@example.com", "G", LocalDateTime.now(clock));
        when(users.findByEmail("test@example.com")).thenReturn(Optional.of(googleUser));

        assertThatThrownBy(() -> service.login(new LoginRequest("test@example.com", User.OAUTH_PASSWORD_PLACEHOLDER)))
                .hasMessage(AuthService.INVALID_PASSWORD);
    }

    @Test
    void googleLoginCreatesTheUserOnFirstSignIn() {
        when(google.verify("id-token")).thenReturn(new GoogleIdentity("g@example.com", "Google User", true));
        when(users.findByEmail("g@example.com")).thenReturn(Optional.empty());

        String token = service.loginWithGoogle("id-token");

        assertThat(jwt.verify(token)).contains("g@example.com");
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        assertThat(saved.getValue().getPasswordHash()).isEqualTo(User.OAUTH_PASSWORD_PLACEHOLDER);
        assertThat(saved.getValue().getName()).isEqualTo("Google User");
    }

    @Test
    void googleLoginRejectsUnverifiedEmail() {
        when(google.verify("id-token")).thenReturn(new GoogleIdentity("g@example.com", "G", false));

        assertThatThrownBy(() -> service.loginWithGoogle("id-token")).isInstanceOf(BadRequestException.class);
        verify(users, never()).save(any());
    }
}
