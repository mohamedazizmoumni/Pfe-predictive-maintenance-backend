package com.yourpackage.business.controller;

import com.yourpackage.business.dto.CompareRequestDTO;
import com.yourpackage.business.dto.CostComparisonDTO;
import com.yourpackage.business.service.CostComparisonService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/costs")
public class CostController {

        private final CostComparisonService costComparisonService;

        public CostController(CostComparisonService costComparisonService) {
                this.costComparisonService = costComparisonService;
    }

    @PostMapping("/compare")
    public ResponseEntity<CostComparisonDTO> compareCosts(@RequestBody @Valid CompareRequestDTO compareRequest) {
                CostComparisonDTO report = costComparisonService.compare(compareRequest);
        return ResponseEntity.ok(report);
    }

        // Compatibility endpoint for frontend flows that only know the machine id.
        @GetMapping("/compare/machine/{machineId}")
        public ResponseEntity<CostComparisonDTO> compareLatestForMachine(@PathVariable Long machineId,
                                                                                                                                         @RequestParam(value = "estimatedFailureDowntimeHours", defaultValue = "8.0")
                                                                                                                                         @Positive Double estimatedFailureDowntimeHours) {
                CostComparisonDTO report = costComparisonService.compareLatestForMachine(machineId, estimatedFailureDowntimeHours);
                return ResponseEntity.ok(report);
        }
}
