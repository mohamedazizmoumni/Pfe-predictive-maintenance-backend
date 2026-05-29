package com.pfe.predictive.finance.mapper;

import com.pfe.predictive.core.entity.finance.AnnualBudget;
import com.pfe.predictive.finance.dto.BudgetResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Pure mapping component — no side effects, no service calls.
 */
@Component
public class AnnualBudgetMapper {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public BudgetResponse toResponse(AnnualBudget entity) {
        if (entity == null) {
            return null;
        }
        return BudgetResponse.builder()
                .id(entity.getId())
                .year(entity.getYear())
                .totalBudget(entity.getTotalBudget())
                .spentAmount(entity.getSpentAmount())
                .remainingBudget(entity.getRemainingBudget())
                .utilizationPercentage(entity.getUtilizationPercentage())
                .notes(entity.getNotes())
                .createdBy(entity.getCreatedBy())
                .createdDate(format(entity.getCreatedDate()))
                .lastModifiedDate(format(entity.getLastModifiedDate()))
                .build();
    }

    private String format(LocalDateTime dt) {
        return dt != null ? dt.format(FORMATTER) : null;
    }
}
