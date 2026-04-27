package com.yourpackage.business.controller;

import com.yourpackage.business.dto.MaintenanceRecommendationDTO;
import com.yourpackage.business.dto.RecommendationRequestDTO;
import com.yourpackage.business.service.MaintenanceRecommendationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recommendations")
public class RecommendationController {

    private final MaintenanceRecommendationService recommendationService;

    public RecommendationController(MaintenanceRecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @PostMapping("/generate")
    public ResponseEntity<MaintenanceRecommendationDTO> generate(@RequestBody @Valid RecommendationRequestDTO request) {
        MaintenanceRecommendationDTO recommendation = recommendationService.generateRecommendation(request);
        return ResponseEntity.ok(recommendation);
    }

    @GetMapping("/machine/{machineId}")
    public ResponseEntity<MaintenanceRecommendationDTO> previewForMachine(@PathVariable Long machineId) {
        RecommendationRequestDTO request = RecommendationRequestDTO.builder()
                .machineId(machineId)
                .failureProbability(0.5)
                .daysUntilPredictedFailure(7)
                .requiredPartIds(List.of())
                .build();

        MaintenanceRecommendationDTO recommendation = recommendationService.generateRecommendation(request);
        return ResponseEntity.ok(recommendation);
    }
}
