package com.expensetracker.expense_tracker.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.expensetracker.expense_tracker.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ForecastService {

    private final TransactionRepository txRepo;

    public List<Map<String, Object>> forecast(String email, int futureMonths) {
        // 1. Fetch last 6 months of historical expense data
        List<Object[]> history = txRepo.monthlyTrend(email, 6);

        // Need at least 2 data points to fit a line
        if (history.size() < 2) {
            return Collections.emptyList();
        }

        // 2. Convert to arrays for regression
        // x = month index (1, 2, 3...), y = total expense
        int n = history.size();
        double[] x = new double[n];
        double[] y = new double[n];

        for (int i = 0; i < n; i++) {
            x[i] = i + 1;
            // row[1] is total_expense from the SQL query
            Object expenseVal = history.get(i)[1];
            y[i] = expenseVal != null ? ((Number) expenseVal).doubleValue() : 0.0;
        }

        // 3. Calculate Linear Regression: y = slope * x + intercept
        // slope  = (n*Σxy - Σx*Σy) / (n*Σx² - (Σx)²)
        // intercept = (Σy - slope*Σx) / n
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        for (int i = 0; i < n; i++) {
            sumX  += x[i];
            sumY  += y[i];
            sumXY += x[i] * y[i];
            sumX2 += x[i] * x[i];
        }

        double slope     = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        double intercept = (sumY - slope * sumX) / n;

        // 4. Predict the next N months
        List<Map<String, Object>> predictions = new ArrayList<>();

        // Figure out what month the last historical data point is
        String lastMonth = (String) history.get(n - 1)[0]; // format: "YYYY-MM"
        LocalDate lastDate = LocalDate.parse(lastMonth + "-01",
                DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        for (int i = 1; i <= futureMonths; i++) {
            double predictedX     = n + i;
            double predictedValue = slope * predictedX + intercept;

            // Don't predict negative expenses
            predictedValue = Math.max(predictedValue, 0);

            LocalDate futureDate = lastDate.plusMonths(i);
            String futureMonth   = futureDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));

            Map<String, Object> point = new LinkedHashMap<>();
            point.put("month",           futureMonth);
            point.put("predictedExpense", Math.round(predictedValue * 100.0) / 100.0);
            point.put("isForecast",       true);
            predictions.add(point);
        }

        return predictions;
    }
}