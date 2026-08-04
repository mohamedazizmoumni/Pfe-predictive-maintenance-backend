package com.pfe.predictive.finance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
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
public class MaintenanceRapportRequest {

    private Long taskId;

    @NotNull(message = "Machine is required")
    private Long machineId;

    private String machineName;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotBlank(message = "Work performed is required")
    private String workPerformed;

    private String partsReplaced;

    @NotNull(message = "Labor hours is required")
    @PositiveOrZero(message = "Labor hours must not be negative")
    private Double laborHours;

    @NotNull(message = "Labor cost is required")
    @PositiveOrZero(message = "Labor cost must not be negative")
    private BigDecimal laborCost;

    @Valid
    @Builder.Default
    private List<RapportPartRequest> parts = List.of();

    @Valid
    @Builder.Default
    private List<ChecklistItemRequest> checklistItems = List.of();
}
