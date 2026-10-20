package com.clarify.auth;

import jakarta.validation.constraints.NotBlank;

/** {@code credential} is the ID token returned by Google Identity Services. */
public record GoogleLoginRequest(@NotBlank String credential) {
}
