package com.clarify.security;

import java.util.UUID;

/** The authenticated principal, available in controllers via {@code @AuthenticationPrincipal}. */
public record CurrentUser(UUID id, String email) {
}
