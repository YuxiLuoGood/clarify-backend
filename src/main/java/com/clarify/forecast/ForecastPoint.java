package com.clarify.forecast;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/** {@code month} is "YYYY-MM". */
public record ForecastPoint(
        String month,
        BigDecimal predictedExpense,
        // Explicit name: bean-style introspection would otherwise drop the "is" prefix.
        @JsonProperty("isForecast") boolean isForecast) {
}
