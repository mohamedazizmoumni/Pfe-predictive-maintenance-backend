package com.pfe.predictive.portal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponse {
    private Long id;
    private Long customerUserId;
    private Long machineId;
    private String machineName;
    private String invoiceNumber;
    private BigDecimal amount;
    private String status;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private String description;
}
