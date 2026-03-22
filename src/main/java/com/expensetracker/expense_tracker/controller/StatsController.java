package com.expensetracker.expense_tracker.controller;

import com.expensetracker.expense_tracker.dto.MonthlyStatsResponse;
import com.expensetracker.expense_tracker.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    // GET /api/stats/monthly?month=2024-03
    @GetMapping("/monthly")
    public MonthlyStatsResponse monthly(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam String month) {
        return statsService.monthly(user.getUsername(), month);
    }

    // GET /api/stats/trend?months=6
    @GetMapping("/trend")
    public List<Map<String, Object>> trend(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(defaultValue = "6") int months) {
        return statsService.trend(user.getUsername(), months);
    }
}