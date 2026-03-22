package com.expensetracker.expense_tracker.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.Map;

@Data
@AllArgsConstructor
public class MonthlyStatsResponse {
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal netSavings;
    private Map<String, BigDecimal> byCategory;
}