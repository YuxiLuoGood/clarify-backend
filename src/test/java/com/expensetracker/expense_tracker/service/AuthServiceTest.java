package com.expensetracker.expense_tracker.service;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.expensetracker.expense_tracker.dto.LoginRequest;
import com.expensetracker.expense_tracker.dto.RegisterRequest;
import com.expensetracker.expense_tracker.model.User;
import com.expensetracker.expense_tracker.repository.UserRepository;
import com.expensetracker.expense_tracker.security.JwtUtil;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepo;
    @Mock private PasswordEncoder encoder;
    @Mock private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerReq;
    private LoginRequest loginReq;

    @BeforeEach
    void setUp() {
        registerReq = new RegisterRequest();
        registerReq.setEmail("test@example.com");
        registerReq.setPassword("password123");
        registerReq.setName("Test User");

        loginReq = new LoginRequest();
        loginReq.setEmail("test@example.com");
        loginReq.setPassword("password123");
    }

    // ── Register Tests ──────────────────────────────────────────

    @Test
    void register_success_returnsToken() {
        when(userRepo.existsByEmail("test@example.com")).thenReturn(false);
        when(encoder.encode("password123")).thenReturn("hashed");
        when(userRepo.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtUtil.generateToken("test@example.com")).thenReturn("jwt-token");

        String token = authService.register(registerReq);

        assertEquals("jwt-token", token);
        verify(userRepo).save(any(User.class));
    }

    @Test
    void register_duplicateEmail_throwsException() {
        when(userRepo.existsByEmail("test@example.com")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> authService.register(registerReq));

        assertEquals("Email already registered", ex.getMessage());
        verify(userRepo, never()).save(any());
    }

    // ── Login Tests ──────────────────────────────────────────────

    @Test
    void login_success_returnsToken() {
        User user = User.builder()
            .email("test@example.com")
            .password("hashed")
            .name("Test User")
            .build();

        when(userRepo.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(encoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtUtil.generateToken("test@example.com")).thenReturn("jwt-token");

        String token = authService.login(loginReq);

        assertEquals("jwt-token", token);
    }

    @Test
    void login_userNotFound_throwsException() {
        when(userRepo.findByEmail("test@example.com")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> authService.login(loginReq));

        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void login_wrongPassword_throwsException() {
        User user = User.builder()
            .email("test@example.com")
            .password("hashed")
            .name("Test User")
            .build();

        when(userRepo.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(encoder.matches("password123", "hashed")).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> authService.login(loginReq));

        assertEquals("Invalid password", ex.getMessage());
    }
}