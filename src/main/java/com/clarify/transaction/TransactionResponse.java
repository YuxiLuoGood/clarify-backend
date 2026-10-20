package com.clarify.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        BigDecimal amount,
        Category category,
        String description,
        TransactionType type,
        LocalDate date,
        LocalDateTime createdAt) {

    static TransactionResponse from(Transaction tx) {
        return new TransactionResponse(tx.getId(), tx.getAmount(), tx.getCategory(), tx.getDescription(),
                tx.getType(), tx.getDate(), tx.getCreatedAt());
    }
}
