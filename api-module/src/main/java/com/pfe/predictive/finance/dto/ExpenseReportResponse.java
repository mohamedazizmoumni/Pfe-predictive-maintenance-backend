package com.pfe.predictive.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseReportResponse {

    private Long id;
    private String title;
    private String description;
    private BigDecimal amount;
    private String category;
    private Long machineId;
    private String machineName;
    private Long maintenanceTaskId;
    private String submittedBy;
    private String submittedByName;
    private String status;
    private String reviewedBy;
    private String reviewedDate;
    private String reviewNote;
    private String rejectionReason;
    private String createdDate;
    private String lastModifiedDate;
}
