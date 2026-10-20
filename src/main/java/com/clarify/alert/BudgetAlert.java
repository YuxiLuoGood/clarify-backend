package com.clarify.alert;

import com.clarify.transaction.Category;

import java.math.BigDecimal;
import java.util.Locale;

/** Message pushed to the browser when a category goes over its monthly budget. */
public record BudgetAlert(String type, Category category, BigDecimal spent, BigDecimal budget, String message) {

    public static final String BUDGET_EXCEEDED = "BUDGET_EXCEEDED";

    public static BudgetAlert exceeded(Category category, BigDecimal spent, BigDecimal budget) {
        // Locale.ROOT: always "1234.50", never "1234,50"
        String message = String.format(Locale.ROOT,
                "⚠️ You've spent $%.2f on %s this month (budget: $%.2f)", spent, category, budget);
        return new BudgetAlert(BUDGET_EXCEEDED, category, spent, budget, message);
    }
}
