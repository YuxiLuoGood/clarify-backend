package com.clarify.forecast;

import com.clarify.stats.StatsService;
import com.clarify.stats.TrendPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForecastServiceTest {

    private static final UUID USER = UUID.randomUUID();

    @Mock StatsService statsService;
    @InjectMocks ForecastService forecastService;

    private static TrendPoint point(String month, String expense) {
        return new TrendPoint(month, new BigDecimal(expense), BigDecimal.ZERO);
    }

    @Test
    void extrapolatesTheTrend() {
        when(statsService.trend(USER, ForecastService.HISTORY_MONTHS)).thenReturn(List.of(
                point("2026-01", "800"), point("2026-02", "900"), point("2026-03", "1000")));

        List<ForecastPoint> result = forecastService.forecast(USER, 3);

        assertThat(result).extracting(ForecastPoint::month).containsExactly("2026-04", "2026-05", "2026-06");
        assertThat(result.get(0).predictedExpense()).isEqualByComparingTo("1100.00");
        assertThat(result.get(2).predictedExpense()).isEqualByComparingTo("1300.00");
        assertThat(result).allMatch(ForecastPoint::isForecast);
    }

    @Test
    void rollsOverTheYear() {
        when(statsService.trend(USER, ForecastService.HISTORY_MONTHS)).thenReturn(List.of(
                point("2025-11", "100"), point("2025-12", "100")));

        assertThat(forecastService.forecast(USER, 2)).extracting(ForecastPoint::month)
                .containsExactly("2026-01", "2026-02");
    }

    @Test
    void needsTwoMonthsOfHistory() {
        when(statsService.trend(USER, ForecastService.HISTORY_MONTHS)).thenReturn(List.of(point("2026-01", "500")));

        assertThat(forecastService.forecast(USER, 3)).isEmpty();
    }

    @Test
    void neverPredictsNegativeSpending() {
        when(statsService.trend(USER, ForecastService.HISTORY_MONTHS)).thenReturn(List.of(
                point("2026-01", "300"), point("2026-02", "100"), point("2026-03", "0")));

        assertThat(forecastService.forecast(USER, 3))
                .allSatisfy(p -> assertThat(p.predictedExpense()).isGreaterThanOrEqualTo(BigDecimal.ZERO));
    }
}
