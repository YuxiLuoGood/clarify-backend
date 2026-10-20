package com.clarify.stats;

import com.clarify.transaction.TransactionType;

import java.math.BigDecimal;

/** Query projection: total amount per transaction type. */
public record TypeTotal(TransactionType type, BigDecimal total) {
}
