package com.expensetracker.expense_tracker.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TransactionRequest {

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotBlank
    @Pattern(regexp = "FOOD|TRANSPORT|SHOPPING|BILLS|OTHER")
    private String category;

    private String description;

    @NotBlank
    @Pattern(regexp = "INCOME|EXPENSE")
    private String type;

    @NotNull
    private LocalDate date;
}