package com.pfe.predictive.portal.dto;

import com.pfe.predictive.core.entity.portal.InvoiceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class InvoiceRequest {

    @NotNull(message = "Customer user ID is required")
    private Long customerUserId;

    private Long machineId;

    @NotBlank(message = "Invoice number is required")
    private String invoiceNumber;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    private InvoiceStatus status;

    @NotNull(message = "Issue date is required")
    private LocalDate issueDate;

    @NotNull(message = "Due date is required")
    private LocalDate dueDate;

    private String description;
}
