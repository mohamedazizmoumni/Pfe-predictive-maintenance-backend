package com.pfe.predictive.explainability.controller;

import com.pfe.predictive.explainability.dto.PredictionExplanation;
import com.pfe.predictive.explainability.service.PredictionExplanationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/machines/{machineId}/explain")
@RequiredArgsConstructor
public class PredictionExplanationController {

    private final PredictionExplanationService explanationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<PredictionExplanation> explain(@PathVariable Long machineId) {
        return ResponseEntity.ok(explanationService.explain(machineId));
    }
}
