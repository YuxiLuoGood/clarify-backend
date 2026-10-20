package com.clarify.stats;

import com.clarify.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.YearMonth;

/** Query projection: total amount per (year, month, type). */
public record MonthlyTypeTotal(Integer year, Integer month, TransactionType type, BigDecimal total) {

    public YearMonth yearMonth() {
        return YearMonth.of(year, month);
    }
}
