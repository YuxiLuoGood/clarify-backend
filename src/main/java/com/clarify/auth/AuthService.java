package com.clarify.auth;

import com.clarify.common.error.BadRequestException;
import com.clarify.security.JwtService;
import com.clarify.user.User;
import com.clarify.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class AuthService {

    // The frontend maps these exact messages to field-level errors.
    public static final String EMAIL_TAKEN = "Email already registered";
    public static final String USER_NOT_FOUND = "User not found";
    public static final String INVALID_PASSWORD = "Invalid password";

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GoogleTokenVerifier googleVerifier;
    private final Clock clock;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService,
                       GoogleTokenVerifier googleVerifier, Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.googleVerifier = googleVerifier;
        this.clock = clock;
    }

    @Transactional
    public String register(RegisterRequest request) {
        if (users.existsByEmail(request.email())) {
            throw new BadRequestException(EMAIL_TAKEN);
        }
        User user = User.withPassword(request.email(), passwordEncoder.encode(request.password()),
                request.name(), LocalDateTime.now(clock));
        try {
            users.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // Lost a race with a concurrent signup for the same address.
            throw new BadRequestException(EMAIL_TAKEN);
        }
        return jwtService.issue(user.getEmail());
    }

    @Transactional(readOnly = true)
    public String login(LoginRequest request) {
        User user = users.findByEmail(request.email())
                .orElseThrow(() -> new BadRequestException(USER_NOT_FOUND));
        if (!passwordMatches(request.password(), user.getPasswordHash())) {
            throw new BadRequestException(INVALID_PASSWORD);
        }
        return jwtService.issue(user.getEmail());
    }

    /**
     * Not @Transactional on purpose: the call to Google happens first, so no
     * database connection is held while waiting on the network.
     */
    public String loginWithGoogle(String credential) {
        GoogleIdentity identity = googleVerifier.verify(credential);
        // An unverified address could otherwise be used to take over an
        // existing password account with the same email.
        if (!identity.emailVerified()) {
            throw new BadRequestException("Google account email is not verified");
        }
        if (users.findByEmail(identity.email()).isEmpty()) {
            users.save(User.fromGoogle(identity.email(), identity.name(), LocalDateTime.now(clock)));
        }
        return jwtService.issue(identity.email());
    }

    private boolean passwordMatches(String raw, String hash) {
        try {
            return passwordEncoder.matches(raw, hash);
        } catch (IllegalArgumentException e) {
            // Recent Spring Security rejects inputs longer than bcrypt's 72-byte limit.
            return false;
        }
    }
}
