package com.clarify.transaction;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Body of POST and PUT /api/transactions. Unknown enum values are rejected by Jackson. */
public record TransactionRequest(
        @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal amount,
        @NotNull Category category,
        @Size(max = 255) String description,
        @NotNull TransactionType type,
        @NotNull LocalDate date) {
}
