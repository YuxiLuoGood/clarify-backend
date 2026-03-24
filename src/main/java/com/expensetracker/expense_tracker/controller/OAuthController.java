package com.expensetracker.expense_tracker.controller;

import com.expensetracker.expense_tracker.service.OAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class OAuthController {

    private final OAuthService oAuthService;

    // POST /api/auth/google
    // Body: { "credential": "<Google ID Token>" }
    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> body) {
        String credential = body.get("credential");
        if (credential == null || credential.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Missing credential"));
        }
        String token = oAuthService.loginWithGoogle(credential);
        return ResponseEntity.ok(Map.of("token", token));
    }
}