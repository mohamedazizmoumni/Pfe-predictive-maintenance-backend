package com.pfe.predictive.maintenancecost.dto;

import com.pfe.predictive.core.entity.MaintenanceRecommendationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedRecommendationResponse {
    private Long id;
    private Long machineId;
    private String machineName;
    private String urgencyLevel;
    private String recommendedAction;
    private String justification;
    private Double failureProbability;
    private Integer daysUntilFailure;
    private MaintenanceRecommendationStatus status;
    private LocalDateTime generatedAt;
    private String decidedBy;
    private LocalDateTime decidedAt;
    private String decisionNote;
    private Long resultingMaintenanceId;
}
