package com.clarify.alert;

import com.clarify.transaction.Category;
import com.clarify.transaction.TransactionCreatedEvent;
import com.clarify.transaction.TransactionRepository;
import com.clarify.transaction.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BudgetAlertServiceTest {

    private static final UUID USER = UUID.randomUUID();
    private static final String EMAIL = "test@example.com";
    private static final LocalDate OCT_1 = LocalDate.of(2026, 10, 1);
    private static final LocalDate NOV_1 = LocalDate.of(2026, 11, 1);

    private TransactionRepository transactions;
    private AlertSessionRegistry registry;
    private BudgetAlertService service;

    @BeforeEach
    void setUp() {
        transactions = mock(TransactionRepository.class);
        registry = mock(AlertSessionRegistry.class);
        var budget = new BudgetProperties(new BigDecimal("500"), Map.of(Category.FOOD, new BigDecimal("300")));
        var clock = Clock.fixed(Instant.parse("2026-10-20T12:00:00Z"), ZoneOffset.UTC);
        service = new BudgetAlertService(transactions, budget, registry, clock);
    }

    private static TransactionCreatedEvent event(TransactionType type, Category category) {
        return new TransactionCreatedEvent(UUID.randomUUID(), USER, EMAIL, type, category,
                BigDecimal.TEN, LocalDate.of(2026, 10, 20));
    }

    private void spentThisMonth(Category category, String amount) {
        when(transactions.sumAmount(USER, TransactionType.EXPENSE, category, OCT_1, NOV_1))
                .thenReturn(amount == null ? null : new BigDecimal(amount));
    }

    @Test
    void alertsWhenTheCategoryGoesOverBudget() {
        spentThisMonth(Category.FOOD, "350.00");

        service.onTransactionCreated(event(TransactionType.EXPENSE, Category.FOOD));

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(registry).send(eq(EMAIL), payload.capture());
        BudgetAlert alert = (BudgetAlert) payload.getValue();
        assertThat(alert.type()).isEqualTo("BUDGET_EXCEEDED");
        assertThat(alert.spent()).isEqualByComparingTo("350");
        assertThat(alert.budget()).isEqualByComparingTo("300");
        assertThat(alert.message()).isEqualTo("⚠️ You've spent $350.00 on FOOD this month (budget: $300.00)");
    }

    @Test
    void staysQuietWithinBudget() {
        spentThisMonth(Category.FOOD, "300.00"); // equal to the limit is still fine

        service.onTransactionCreated(event(TransactionType.EXPENSE, Category.FOOD));

        verify(registry, never()).send(any(), any());
    }

    @Test
    void usesTheDefaultLimitForUnconfiguredCategories() {
        spentThisMonth(Category.SHOPPING, "450");

        service.onTransactionCreated(event(TransactionType.EXPENSE, Category.SHOPPING));

        verify(registry, never()).send(any(), any()); // default limit is 500
    }

    @Test
    void ignoresIncome() {
        service.onTransactionCreated(event(TransactionType.INCOME, Category.OTHER));

        verifyNoInteractions(transactions, registry);
    }

    @Test
    void neverPropagatesFailures() {
        when(transactions.sumAmount(any(), any(), any(), any(), any())).thenThrow(new IllegalStateException("db down"));

        assertThatCode(() -> service.onTransactionCreated(event(TransactionType.EXPENSE, Category.FOOD)))
                .doesNotThrowAnyException();
    }
}
