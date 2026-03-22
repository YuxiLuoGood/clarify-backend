package com.expensetracker.expense_tracker.controller;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.expensetracker.expense_tracker.service.ForecastService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/forecast")
@RequiredArgsConstructor
public class ForecastController {

    private final ForecastService forecastService;

    // GET /api/forecast?months=3
    @GetMapping
    public List<Map<String, Object>> forecast(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(defaultValue = "3") int months) {
        return forecastService.forecast(user.getUsername(), months);
    }
}