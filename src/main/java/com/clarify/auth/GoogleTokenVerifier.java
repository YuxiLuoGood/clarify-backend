package com.clarify.auth;

/**
 * Port for Google ID token verification, so {@link AuthService} can be
 * tested without network access to Google.
 */
public interface GoogleTokenVerifier {

    /**
     * @throws com.clarify.common.error.BadRequestException if the token is not
     *         a valid ID token for this app
     */
    GoogleIdentity verify(String idToken);
}
