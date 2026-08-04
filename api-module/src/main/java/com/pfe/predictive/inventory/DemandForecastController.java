package com.pfe.predictive.inventory;

import com.pfe.predictive.inventory.dto.DemandForecastResponse;
import com.pfe.predictive.inventory.service.DemandForecastService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory/demand-forecast")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STOCK_MANAGER', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
@Tag(name = "Demand Forecast", description = "Projected days-until-stockout per part, from real consumption history")
public class DemandForecastController {

    private final DemandForecastService demandForecastService;

    @GetMapping
    @Operation(summary = "Get demand forecast for all parts, soonest stockout first")
    public ResponseEntity<List<DemandForecastResponse>> getForecast() {
        return ResponseEntity.ok(demandForecastService.getForecast());
    }
}
