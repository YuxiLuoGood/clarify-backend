package com.clarify.stats;

import com.clarify.transaction.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static com.clarify.transaction.TransactionType.EXPENSE;
import static com.clarify.transaction.TransactionType.INCOME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StatsServiceTest {

    private static final UUID USER = UUID.randomUUID();
    private static final ZoneId ZONE = ZoneId.of("UTC");

    private StatsQueries queries;
    private StatsService service;

    @BeforeEach
    void setUp() {
        queries = mock(StatsQueries.class);
        Clock clock = Clock.fixed(ZonedDateTime.of(2026, 10, 20, 12, 0, 0, 0, ZONE).toInstant(), ZONE);
        service = new StatsService(queries, clock);
    }

    @Test
    void trendCoversTheLastNMonthsAndFillsGaps() {
        // 6 months up to and including October 2026: May 1 .. Nov 1 (exclusive)
        when(queries.monthlyTotals(USER, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 11, 1))).thenReturn(List.of(
                new MonthlyTypeTotal(2026, 7, EXPENSE, new BigDecimal("100")),
                new MonthlyTypeTotal(2026, 10, EXPENSE, new BigDecimal("400")),
                new MonthlyTypeTotal(2026, 10, INCOME, new BigDecimal("2000"))));

        List<TrendPoint> trend = service.trend(USER, 6);

        assertThat(trend).extracting(TrendPoint::month).containsExactly("2026-07", "2026-08", "2026-09", "2026-10");
        assertThat(trend.get(1).totalExpense()).isEqualByComparingTo("0");
        assertThat(trend.get(3).totalExpense()).isEqualByComparingTo("400");
        assertThat(trend.get(3).totalIncome()).isEqualByComparingTo("2000");
    }

    @Test
    void trendIsEmptyWithoutData() {
        when(queries.monthlyTotals(USER, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 11, 1))).thenReturn(List.of());

        assertThat(service.trend(USER, 6)).isEmpty();
    }

    @Test
    void monthlySummary() {
        LocalDate from = LocalDate.of(2026, 3, 1);
        LocalDate to = LocalDate.of(2026, 4, 1);
        when(queries.totalsByType(USER, from, to)).thenReturn(List.of(
                new TypeTotal(INCOME, new BigDecimal("3000")),
                new TypeTotal(EXPENSE, new BigDecimal("1100.50"))));
        when(queries.expensesByCategory(USER, from, to)).thenReturn(List.of(
                new CategoryTotal(Category.BILLS, new BigDecimal("900")),
                new CategoryTotal(Category.FOOD, new BigDecimal("200.50"))));

        MonthlyStatsResponse stats = service.monthly(USER, java.time.YearMonth.of(2026, 3));

        assertThat(stats.totalIncome()).isEqualByComparingTo("3000");
        assertThat(stats.netSavings()).isEqualByComparingTo("1899.50");
        assertThat(stats.byCategory()).containsOnlyKeys(Category.BILLS, Category.FOOD);
    }

    @Test
    void monthlySummaryOfAnEmptyMonthIsZero() {
        MonthlyStatsResponse stats = service.monthly(USER, java.time.YearMonth.of(2020, 1));

        assertThat(stats.totalIncome()).isEqualByComparingTo("0");
        assertThat(stats.totalExpense()).isEqualByComparingTo("0");
        assertThat(stats.byCategory()).isEmpty();
    }
}
