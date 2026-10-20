package com.clarify.config;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Browser origins allowed to call the API and open the alert WebSocket. */
@Validated
@ConfigurationProperties(prefix = "clarify.cors")
public record CorsProperties(@NotEmpty List<String> allowedOrigins) {
}
