package com.pfe.predictive.maintenancecost.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Maintenance Report Response
 * Detailed maintenance cost analysis
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceReportResponse {

    private Long totalActions;
    private BigDecimal totalCost;
    private BigDecimal totalLaborCost;
    private BigDecimal totalPartsCost;
    private List<MaintenanceDetail> actions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MaintenanceDetail {
        private Long id;
        private Long machineId;
        private String machineName;
        private String type;
        private String status;
        private Double durationHours;
        private BigDecimal laborCost;
        private BigDecimal partsCost;
        private BigDecimal totalCost;
        private LocalDateTime scheduledDate;
    }
}
