package com.pfe.predictive.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceRapportResponse {

    private Long id;
    private Long taskId;
    private Long machineId;
    private String machineName;
    private String technicianUsername;
    private String technicianName;
    private String title;
    private String description;
    private String workPerformed;
    private String partsReplaced;
    private Double laborHours;
    private BigDecimal laborCost;
    private BigDecimal partsCost;
    private BigDecimal totalCost;
    private String status;
    private List<RapportPartResponse> parts;
    private List<ChecklistItemResponse> checklistItems;
    private boolean hasFailedChecklistItems;
    private String createdDate;
    private String lastModifiedDate;
    private String approvedByManager;
    private String managerApprovedDate;
    private String approvedByFinance;
    private String financeApprovedDate;
    private String rejectionReason;
}
