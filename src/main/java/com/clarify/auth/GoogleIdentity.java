package com.clarify.auth;

/** What we take from a verified Google ID token. */
public record GoogleIdentity(String email, String name, boolean emailVerified) {
}
