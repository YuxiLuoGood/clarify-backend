package com.clarify.stats;

import java.math.BigDecimal;

/** {@code month} is "YYYY-MM". */
public record TrendPoint(String month, BigDecimal totalExpense, BigDecimal totalIncome) {
}
