package com.clarify.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Published when a transaction is saved. Listeners (budget alerts) react to it
 * without TransactionService knowing they exist.
 */
public record TransactionCreatedEvent(
        UUID transactionId,
        UUID userId,
        String userEmail,
        TransactionType type,
        Category category,
        BigDecimal amount,
        LocalDate date) {
}
