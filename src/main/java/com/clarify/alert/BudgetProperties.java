package com.clarify.alert;

import com.clarify.transaction.Category;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.Map;

/** Monthly budget per category, from {@code clarify.budget} — tunable without a code change. */
@Validated
@ConfigurationProperties(prefix = "clarify.budget")
public record BudgetProperties(@NotNull @Positive BigDecimal defaultLimit, Map<Category, BigDecimal> limits) {

    public BudgetProperties {
        limits = limits == null ? Map.of() : Map.copyOf(limits);
    }

    public BigDecimal limitFor(Category category) {
        return limits.getOrDefault(category, defaultLimit);
    }
}
