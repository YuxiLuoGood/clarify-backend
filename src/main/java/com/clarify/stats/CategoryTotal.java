package com.clarify.stats;

import com.clarify.transaction.Category;

import java.math.BigDecimal;

/** Query projection: total expense per category. */
public record CategoryTotal(Category category, BigDecimal total) {
}
