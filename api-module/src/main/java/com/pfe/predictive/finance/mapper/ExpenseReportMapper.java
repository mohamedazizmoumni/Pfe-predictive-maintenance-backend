package com.pfe.predictive.finance.mapper;

import com.pfe.predictive.core.entity.finance.ExpenseReport;
import com.pfe.predictive.finance.dto.ExpenseReportResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Pure mapping component — no side effects, no service calls.
 */
@Component
public class ExpenseReportMapper {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public ExpenseReportResponse toResponse(ExpenseReport entity) {
        if (entity == null) {
            return null;
        }
        return ExpenseReportResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .amount(entity.getAmount())
                .category(entity.getCategory() != null ? entity.getCategory().name() : null)
                .machineId(entity.getMachineId())
                .machineName(entity.getMachineName())
                .maintenanceTaskId(entity.getMaintenanceTaskId())
                .submittedBy(entity.getSubmittedBy())
                .submittedByName(entity.getSubmittedByName())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .reviewedBy(entity.getReviewedBy())
                .reviewedDate(format(entity.getReviewedDate()))
                .reviewNote(entity.getReviewNote())
                .rejectionReason(entity.getRejectionReason())
                .createdDate(format(entity.getCreatedDate()))
                .lastModifiedDate(format(entity.getLastModifiedDate()))
                .build();
    }

    private String format(LocalDateTime dt) {
        return dt != null ? dt.format(FORMATTER) : null;
    }
}
