package com.clarify.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Issues and verifies access tokens: HS256, subject = email, 24h by default.
 * The algorithm is pinned (jjwt would otherwise pick HS384/512 for long
 * secrets) so tokens stay interchangeable with the FastAPI backend.
 */
@Service
public class JwtService {

    private static final int MIN_SECRET_BYTES = 32; // 256 bits, required for HS256

    private final SecretKey key;
    private final Duration expiration;
    private final Clock clock;
    private final JwtParser parser;

    public JwtService(JwtProperties properties, Clock clock) {
        byte[] secret = properties.secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < MIN_SECRET_BYTES) {
            // Fail at startup, not on the first login.
            throw new IllegalStateException("clarify.jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes");
        }
        this.key = new SecretKeySpec(secret, "HmacSHA256");
        this.expiration = properties.expiration();
        this.clock = clock;
        this.parser = Jwts.parser()
                .verifyWith(key)
                .clock(() -> Date.from(this.clock.instant()))
                .build();
    }

    public String issue(String email) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /** The token's email if it is authentic and unexpired; empty otherwise. */
    public Optional<String> verify(String token) {
        try {
            Claims claims = parser.parseSignedClaims(token).getPayload();
            if (claims.getExpiration() == null) return Optional.empty();
            String subject = claims.getSubject();
            return subject == null || subject.isBlank() ? Optional.empty() : Optional.of(subject);
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
