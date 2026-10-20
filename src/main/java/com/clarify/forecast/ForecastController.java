package com.clarify.forecast;

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

import java.util.List;

@RestController
@RequestMapping("/api/forecast")
@Tag(name = "Forecast")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class ForecastController {

    private final ForecastService service;

    public ForecastController(ForecastService service) {
        this.service = service;
    }

    @Operation(summary = "Predicted expenses for the next N months")
    @GetMapping
    public List<ForecastPoint> forecast(@AuthenticationPrincipal CurrentUser user,
                                        @RequestParam(defaultValue = "3") @Min(1) @Max(24) int months) {
        return service.forecast(user.id(), months);
    }
}
