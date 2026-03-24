package com.expensetracker.expense_tracker.service;

import com.expensetracker.expense_tracker.model.User;
import com.expensetracker.expense_tracker.repository.UserRepository;
import com.expensetracker.expense_tracker.security.JwtUtil;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class OAuthService {

    private final UserRepository userRepo;
    private final JwtUtil jwtUtil;

    @Value("${google.client-id}")
    private String googleClientId;

    public String loginWithGoogle(String idTokenString) {
        try {
            // Verify the Google ID token
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(googleClientId))
                .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new RuntimeException("Invalid Google token");
            }

            // Extract user info from token
            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String name  = (String) payload.get("name");

            // Find or create user
            User user = userRepo.findByEmail(email).orElseGet(() -> {
                User newUser = User.builder()
                    .email(email)
                    .name(name != null ? name : email)
                    // Google users don't have a password
                    .password("GOOGLE_OAUTH_" + email)
                    .build();
                return userRepo.save(newUser);
            });

            return jwtUtil.generateToken(user.getEmail());

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Google authentication failed: " + e.getMessage());
        }
    }
}