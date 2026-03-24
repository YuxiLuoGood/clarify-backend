package com.expensetracker.expense_tracker.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.expensetracker.expense_tracker.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class ForecastServiceTest {

    @Mock private TransactionRepository txRepo;

    @InjectMocks
    private ForecastService forecastService;

    private List<Object[]> buildHistory(String[] months, String[] amounts) {
        List<Object[]> history = new ArrayList<>();
        for (int i = 0; i < months.length; i++) {
            history.add(new Object[]{months[i], new BigDecimal(amounts[i])});
        }
        return history;
    }

    @Test
    void forecast_withSufficientData_returnsPredictions() {
        String m1 = LocalDate.now().minusMonths(2).format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String m2 = LocalDate.now().minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String m3 = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        List<Object[]> history = buildHistory(
            new String[]{m1, m2, m3},
            new String[]{"800", "900", "1000"}
        );

        when(txRepo.monthlyTrend(anyString(), anyInt())).thenReturn(history);

        List<Map<String, Object>> result = forecastService.forecast("test@example.com", 3);

        assertEquals(3, result.size());
        result.forEach(p -> assertTrue((Boolean) p.get("isForecast")));
        result.forEach(p -> assertTrue((Double) p.get("predictedExpense") >= 0));
    }

    @Test
    void forecast_withInsufficientData_returnsEmpty() {
        List<Object[]> history = buildHistory(
            new String[]{"2026-01"},
            new String[]{"500"}
        );

        when(txRepo.monthlyTrend(anyString(), anyInt())).thenReturn(history);

        List<Map<String, Object>> result = forecastService.forecast("test@example.com", 3);

        assertTrue(result.isEmpty());
    }

    @Test
    void forecast_neverReturnsNegative() {
        String m1 = LocalDate.now().minusMonths(2).format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String m2 = LocalDate.now().minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String m3 = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        List<Object[]> history = buildHistory(
            new String[]{m1, m2, m3},
            new String[]{"300", "100", "0"}
        );

        when(txRepo.monthlyTrend(anyString(), anyInt())).thenReturn(history);

        List<Map<String, Object>> result = forecastService.forecast("test@example.com", 3);

        result.forEach(p ->
            assertTrue((Double) p.get("predictedExpense") >= 0,
                "Predicted expense should never be negative")
        );
    }
}