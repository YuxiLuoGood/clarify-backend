package com.clarify.forecast;

import com.clarify.stats.StatsService;
import com.clarify.stats.TrendPoint;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class ForecastService {

    static final int HISTORY_MONTHS = 6;

    private final StatsService statsService;

    public ForecastService(StatsService statsService) {
        this.statsService = statsService;
    }

    /** Predicted expense for each of the {@code futureMonths} months after the last month with data. */
    public List<ForecastPoint> forecast(UUID userId, int futureMonths) {
        List<TrendPoint> history = statsService.trend(userId, HISTORY_MONTHS);
        if (history.size() < 2) {
            return Collections.emptyList();
        }

        double[] expenses = history.stream().mapToDouble(p -> p.totalExpense().doubleValue()).toArray();
        LinearRegression model = LinearRegression.fit(expenses);
        YearMonth last = YearMonth.parse(history.getLast().month());

        List<ForecastPoint> points = new ArrayList<>(futureMonths);
        for (int i = 1; i <= futureMonths; i++) {
            double predicted = Math.max(model.predict(expenses.length + i), 0);   // never negative spending
            points.add(new ForecastPoint(
                    last.plusMonths(i).toString(),
                    BigDecimal.valueOf(predicted).setScale(2, RoundingMode.HALF_UP),
                    true));
        }
        return points;
    }
}
