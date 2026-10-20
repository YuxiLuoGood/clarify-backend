package com.clarify.stats;

import com.clarify.transaction.Category;

import java.math.BigDecimal;
import java.util.Map;

public record MonthlyStatsResponse(
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal netSavings,
        Map<Category, BigDecimal> byCategory) {
}
