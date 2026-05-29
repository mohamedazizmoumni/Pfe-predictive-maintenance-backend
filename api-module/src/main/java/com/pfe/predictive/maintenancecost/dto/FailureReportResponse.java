package com.pfe.predictive.maintenancecost.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Failure Report Response
 * Detailed failure cost analysis
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FailureReportResponse {

    private Long totalFailures;
    private BigDecimal totalCost;
    private Double totalDowntimeHours;
    private List<FailureDetail> failures;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailureDetail {
        private Long id;
        private Long machineId;
        private String machineName;
        private String failureType;
        private Double downtimeHours;
        private BigDecimal cost;
        private LocalDateTime occurredAt;
    }
}
