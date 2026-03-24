package com.expensetracker.expense_tracker.service;

import com.expensetracker.expense_tracker.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForecastServiceTest {

    @Mock private TransactionRepository txRepo;

    @InjectMocks
    private ForecastService forecastService;

    @Test
    void forecast_withSufficientData_returnsPredictions() {
        // Build 3 months of mock data
        String m1 = LocalDate.now().minusMonths(2).format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String m2 = LocalDate.now().minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String m3 = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        List<Object[]> history = List.of(
            new Object[]{m1, new BigDecimal("800")},
            new Object[]{m2, new BigDecimal("900")},
            new Object[]{m3, new BigDecimal("1000")}
        );

        when(txRepo.monthlyTrend(anyString(), anyInt())).thenReturn(history);

        List<Map<String, Object>> result = forecastService.forecast("test@example.com", 3);

        // Should return 3 predicted months
        assertEquals(3, result.size());

        // All predictions should be marked as forecast
        result.forEach(p -> assertTrue((Boolean) p.get("isForecast")));

        // Predicted values should be positive
        result.forEach(p -> assertTrue((Double) p.get("predictedExpense") >= 0));
    }

    @Test
    void forecast_withInsufficientData_returnsEmpty() {
        // Only 1 data point — can't fit a line
        when(txRepo.monthlyTrend(anyString(), anyInt()))
            .thenReturn(List.of(new Object[]{"2026-01", new BigDecimal("500")}));

        List<Map<String, Object>> result = forecastService.forecast("test@example.com", 3);

        assertTrue(result.isEmpty());
    }

    @Test
    void forecast_neverReturnsNegative() {
        // Declining trend that would go negative
        String m1 = LocalDate.now().minusMonths(2).format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String m2 = LocalDate.now().minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String m3 = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        List<Object[]> history = List.of(
            new Object[]{m1, new BigDecimal("300")},
            new Object[]{m2, new BigDecimal("100")},
            new Object[]{m3, new BigDecimal("0")}
        );

        when(txRepo.monthlyTrend(anyString(), anyInt())).thenReturn(history);

        List<Map<String, Object>> result = forecastService.forecast("test@example.com", 3);

        result.forEach(p ->
            assertTrue((Double) p.get("predictedExpense") >= 0,
                "Predicted expense should never be negative")
        );
    }
}