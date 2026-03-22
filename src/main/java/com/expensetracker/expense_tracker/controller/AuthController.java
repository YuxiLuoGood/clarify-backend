package com.expensetracker.expense_tracker.controller;

import com.expensetracker.expense_tracker.dto.LoginRequest;
import com.expensetracker.expense_tracker.dto.RegisterRequest;
import com.expensetracker.expense_tracker.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // POST /api/auth/register
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        String token = authService.register(req);
        return ResponseEntity.ok(Map.of("token", token));
    }

    // POST /api/auth/login
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        String token = authService.login(req);
        return ResponseEntity.ok(Map.of("token", token));
    }
}