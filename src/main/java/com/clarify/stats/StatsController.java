package com.clarify.stats;

import com.clarify.config.OpenApiConfig;
import com.clarify.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/stats")
@Tag(name = "Stats")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class StatsController {

    private final StatsService service;

    public StatsController(StatsService service) {
        this.service = service;
    }

    @Operation(summary = "Income, expenses, savings and category breakdown for a month (YYYY-MM)")
    @GetMapping("/monthly")
    public MonthlyStatsResponse monthly(@AuthenticationPrincipal CurrentUser user,
                                        @RequestParam YearMonth month) {
        return service.monthly(user.id(), month);
    }

    @Operation(summary = "Monthly income/expense totals for the last N months")
    @GetMapping("/trend")
    public List<TrendPoint> trend(@AuthenticationPrincipal CurrentUser user,
                                  @RequestParam(defaultValue = "6") @Min(1) @Max(60) int months) {
        return service.trend(user.id(), months);
    }
}
