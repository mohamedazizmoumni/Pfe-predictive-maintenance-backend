package com.pfe.predictive.finance.mapper;

import com.pfe.predictive.core.entity.finance.ChecklistItem;
import com.pfe.predictive.core.entity.finance.MaintenanceRapport;
import com.pfe.predictive.core.entity.finance.RapportPart;
import com.pfe.predictive.finance.dto.ChecklistItemResponse;
import com.pfe.predictive.finance.dto.MaintenanceRapportResponse;
import com.pfe.predictive.finance.dto.RapportPartResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure mapping component — no side effects, no service calls.
 */
@Component
public class MaintenanceRapportMapper {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public MaintenanceRapportResponse toResponse(MaintenanceRapport entity) {
        if (entity == null) {
            return null;
        }
        return MaintenanceRapportResponse.builder()
                .id(entity.getId())
                .taskId(entity.getTaskId())
                .machineId(entity.getMachineId())
                .machineName(entity.getMachineName())
                .technicianUsername(entity.getTechnicianUsername())
                .technicianName(entity.getTechnicianName())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .workPerformed(entity.getWorkPerformed())
                .partsReplaced(entity.getPartsReplaced())
                .laborHours(entity.getLaborHours())
                .laborCost(entity.getLaborCost())
                .partsCost(entity.getPartsCost())
                .totalCost(entity.getTotalCost())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .parts(toPartResponses(entity.getParts()))
                .checklistItems(toChecklistItemResponses(entity.getChecklistItems()))
                .hasFailedChecklistItems(hasFailedItems(entity.getChecklistItems()))
                .createdDate(format(entity.getCreatedDate()))
                .lastModifiedDate(format(entity.getLastModifiedDate()))
                .approvedByManager(entity.getApprovedByManager())
                .managerApprovedDate(format(entity.getManagerApprovedDate()))
                .approvedByFinance(entity.getApprovedByFinance())
                .financeApprovedDate(format(entity.getFinanceApprovedDate()))
                .rejectionReason(entity.getRejectionReason())
                .build();
    }

    private List<RapportPartResponse> toPartResponses(List<RapportPart> parts) {
        if (parts == null) {
            return List.of();
        }
        List<RapportPartResponse> responses = new ArrayList<>();
        long index = 1;
        for (RapportPart part : parts) {
            responses.add(RapportPartResponse.builder()
                    .id(index++)
                    .partName(part.getPartName())
                    .partCode(part.getPartCode())
                    .quantity(part.getQuantity())
                    .unitCost(part.getUnitCost())
                    .totalCost(part.getTotalCost())
                    .supplier(part.getSupplier())
                    .build());
        }
        return responses;
    }

    private List<ChecklistItemResponse> toChecklistItemResponses(List<ChecklistItem> items) {
        if (items == null) {
            return List.of();
        }
        List<ChecklistItemResponse> responses = new ArrayList<>();
        long index = 1;
        for (ChecklistItem item : items) {
            responses.add(ChecklistItemResponse.builder()
                    .id(index++)
                    .description(item.getDescription())
                    .passed(item.isPassed())
                    .notes(item.getNotes())
                    .build());
        }
        return responses;
    }

    private boolean hasFailedItems(List<ChecklistItem> items) {
        return items != null && items.stream().anyMatch(item -> !item.isPassed());
    }

    private String format(LocalDateTime dt) {
        return dt != null ? dt.format(FORMATTER) : null;
    }
}
