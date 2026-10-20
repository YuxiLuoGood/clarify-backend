package com.clarify.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email(regexp = ".+@.+\\..+") @Size(max = 255) String email,
        // bcrypt only looks at the first 72 bytes
        @NotBlank @Size(min = 6, max = 72, message = "must be between 6 and 72 characters") String password,
        @NotBlank @Size(max = 100) String name) {

    public RegisterRequest {
        if (email != null) email = email.strip();
        if (name != null) name = name.strip();
    }
}
