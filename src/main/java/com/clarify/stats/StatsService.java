package com.clarify.stats;

import com.clarify.transaction.Category;
import com.clarify.transaction.TransactionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class StatsService {

    private final StatsQueries queries;
    private final Clock clock;

    public StatsService(StatsQueries queries, Clock clock) {
        this.queries = queries;
        this.clock = clock;
    }

    public MonthlyStatsResponse monthly(UUID userId, YearMonth month) {
        var from = month.atDay(1);
        var to = month.plusMonths(1).atDay(1);

        Map<TransactionType, BigDecimal> byType = new EnumMap<>(TransactionType.class);
        queries.totalsByType(userId, from, to).forEach(t -> byType.put(t.type(), t.total()));
        BigDecimal income = byType.getOrDefault(TransactionType.INCOME, BigDecimal.ZERO);
        BigDecimal expense = byType.getOrDefault(TransactionType.EXPENSE, BigDecimal.ZERO);

        Map<Category, BigDecimal> byCategory = new LinkedHashMap<>();   // keeps the query's largest-first order
        queries.expensesByCategory(userId, from, to).forEach(c -> byCategory.put(c.category(), c.total()));

        return new MonthlyStatsResponse(income, expense, income.subtract(expense), byCategory);
    }

    /**
     * Monthly totals for the last {@code months} calendar months, current month
     * included, oldest first. Future-dated transactions are ignored.
     * <p>
     * Empty months between the first and last month with data are filled with
     * zeros so the series — and the regression built on it — has no gaps.
     * Empty months before the user's first transaction are left out rather
     * than counted as zero spending.
     */
    public List<TrendPoint> trend(UUID userId, int months) {
        YearMonth current = YearMonth.now(clock);
        YearMonth first = current.minusMonths(months - 1L);

        TreeMap<YearMonth, BigDecimal[]> totals = new TreeMap<>();  // [expense, income]
        for (MonthlyTypeTotal row : queries.monthlyTotals(userId, first.atDay(1), current.plusMonths(1).atDay(1))) {
            BigDecimal[] slot = totals.computeIfAbsent(row.yearMonth(), m -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            slot[row.type() == TransactionType.EXPENSE ? 0 : 1] = row.total();
        }
        if (totals.isEmpty()) {
            return Collections.emptyList();
        }

        List<TrendPoint> series = new ArrayList<>();
        for (YearMonth m = totals.firstKey(); !m.isAfter(totals.lastKey()); m = m.plusMonths(1)) {
            BigDecimal[] slot = totals.getOrDefault(m, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            series.add(new TrendPoint(m.toString(), slot[0], slot[1]));
        }
        return series;
    }
}
