package com.pfe.predictive.inventory.mapper;

import com.pfe.predictive.inventory.dto.InventoryUsageRequest;
import com.pfe.predictive.inventory.dto.InventoryUsageResponse;
import com.pfe.predictive.inventory.entity.InventoryUsage;
import com.pfe.predictive.inventory.entity.Part;
import org.springframework.stereotype.Component;

@Component
public class InventoryUsageMapper {

    public InventoryUsageResponse toResponse(InventoryUsage usage) {
        if (usage == null) {
            return null;
        }

        return InventoryUsageResponse.builder()
            .id(usage.getId())
            .partId(usage.getPart().getId())
            .partName(usage.getPart().getName())
            .quantityUsed(usage.getQuantityUsed())
            .taskId(usage.getTaskId())
            .reason(usage.getReason())
            .usedBy(usage.getUsedBy())
            .usedDate(usage.getUsedDate() != null ? usage.getUsedDate().toString() : null)
            .notes(usage.getNotes())
            .build();
    }

    public InventoryUsage toEntity(InventoryUsageRequest request, Part part) {
        if (request == null || part == null) {
            return null;
        }

        return InventoryUsage.builder()
            .part(part)
            .quantityUsed(request.getQuantityUsed())
            .taskId(request.getTaskId())
            .reason(request.getReason())
            .notes(request.getNotes())
            .build();
    }
}
