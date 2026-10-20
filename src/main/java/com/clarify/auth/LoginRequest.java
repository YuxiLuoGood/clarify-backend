package com.clarify.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank @Email(regexp = ".+@.+\\..+") String email,
        @NotBlank String password) {

    public LoginRequest {
        if (email != null) email = email.strip();
    }
}
