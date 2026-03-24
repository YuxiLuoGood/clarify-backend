package com.expensetracker.expense_tracker.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.expensetracker.expense_tracker.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BudgetAlertService {

    private final SimpMessagingTemplate messagingTemplate;
    private final TransactionRepository txRepo;

    // Default budget thresholds per category (USD)
    private static final Map<String, BigDecimal> THRESHOLDS = new HashMap<>() {{
        put("FOOD",      new BigDecimal("300"));
        put("TRANSPORT", new BigDecimal("200"));
        put("SHOPPING",  new BigDecimal("500"));
        put("BILLS",     new BigDecimal("1000"));
        put("OTHER",     new BigDecimal("200"));
    }};

    // Called after every new transaction is saved
    public void checkAndAlert(String email, String category) {
        String month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        // Get current month total for this category
        BigDecimal total = txRepo.sumByCategory(email, month)
            .stream()
            .filter(row -> category.equals(row[0]))
            .map(row -> (BigDecimal) row[1])
            .findFirst()
            .orElse(BigDecimal.ZERO);

        BigDecimal threshold = THRESHOLDS.getOrDefault(category, new BigDecimal("500"));

        if (total.compareTo(threshold) > 0) {
            Map<String, Object> alert = new HashMap<>();
            alert.put("type", "BUDGET_EXCEEDED");
            alert.put("category", category);
            alert.put("spent", total);
            alert.put("budget", threshold);
            alert.put("message", String.format(
                "⚠️ You've spent $%.2f on %s this month (budget: $%.2f)",
                total, category, threshold
            ));

            // Push alert to this user's personal topic
            messagingTemplate.convertAndSend(
                "/topic/alerts/" + email.replace("@", "_").replace(".", "_"),
                alert
            );
        }
    }
}