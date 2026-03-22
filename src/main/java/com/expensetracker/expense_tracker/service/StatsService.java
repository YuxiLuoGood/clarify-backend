package com.expensetracker.expense_tracker.service;

import com.expensetracker.expense_tracker.dto.MonthlyStatsResponse;
import com.expensetracker.expense_tracker.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final TransactionRepository txRepo;

    // 某个月的收支总结
    public MonthlyStatsResponse monthly(String email, String month) {
        BigDecimal income  = txRepo.sumByType(email, "INCOME",  month);
        BigDecimal expense = txRepo.sumByType(email, "EXPENSE", month);
        BigDecimal net     = income.subtract(expense);

        // 按类别统计支出
        Map<String, BigDecimal> byCategory = new LinkedHashMap<>();
        txRepo.sumByCategory(email, month)
              .forEach(row -> byCategory.put(
                  (String) row[0],
                  (BigDecimal) row[1]
              ));

        return new MonthlyStatsResponse(income, expense, net, byCategory);
    }

    // 最近N个月的趋势
    public List<Map<String, Object>> trend(String email, int months) {
        List<Map<String, Object>> result = new ArrayList<>();

        txRepo.monthlyTrend(email, months).forEach(row -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("month",        row[0]);
            m.put("totalExpense", row[1]);
            m.put("totalIncome",  row[2]);
            result.add(m);
        });

        return result;
    }
}