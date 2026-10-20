package com.clarify.auth;

import com.clarify.common.error.BadRequestException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

/**
 * Verifies signature, expiry, issuer and audience with Google's client library.
 * One verifier is built and reused: it caches Google's signing keys, whereas
 * building one per request re-downloads them every time.
 */
@Component
public class GoogleApiTokenVerifier implements GoogleTokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(GoogleApiTokenVerifier.class);

    private final GoogleIdTokenVerifier verifier;

    public GoogleApiTokenVerifier(GoogleProperties properties) {
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(List.of(properties.clientId()))
                .build();
    }

    @Override
    public GoogleIdentity verify(String idToken) {
        GoogleIdToken token;
        try {
            token = verifier.verify(idToken);
        } catch (IllegalArgumentException e) {        // not even shaped like a JWT
            throw new BadRequestException("Invalid Google token");
        } catch (GeneralSecurityException | IOException e) {
            log.warn("Google token verification failed: {}", e.toString());
            throw new BadRequestException("Google authentication failed");
        }
        if (token == null) {
            throw new BadRequestException("Invalid Google token");
        }

        GoogleIdToken.Payload payload = token.getPayload();
        if (payload.getEmail() == null) {
            throw new BadRequestException("Google account has no email address");
        }
        return new GoogleIdentity(
                payload.getEmail(),
                (String) payload.get("name"),
                Boolean.TRUE.equals(payload.getEmailVerified()));
    }
}
