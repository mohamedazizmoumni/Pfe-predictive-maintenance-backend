package com.pfe.predictive.inventory.mapper;

import com.pfe.predictive.inventory.dto.PartRequest;
import com.pfe.predictive.inventory.dto.PartResponse;
import com.pfe.predictive.inventory.entity.Part;
import com.pfe.predictive.inventory.entity.PartStatus;
import org.springframework.stereotype.Component;

@Component
public class PartMapper {

    public PartResponse toResponse(Part part) {
        if (part == null) {
            return null;
        }

        return PartResponse.builder()
            .id(part.getId())
            .name(part.getName())
            .description(part.getDescription())
            .partNumber(part.getPartNumber())
            .category(part.getCategory())
            .cost(part.getCost())
            .currentStock(part.getCurrentStock())
            .minimumStock(part.getMinimumStock())
            .reorderQuantity(part.getReorderQuantity())
            .unit(part.getUnit())
            .supplier(part.getSupplier())
            .status(part.getStatus() != null ? part.getStatus().toString() : PartStatus.AVAILABLE.toString())
            .notes(part.getNotes())
            .createdDate(part.getCreatedDate() != null ? part.getCreatedDate().toString() : null)
            .lastModifiedDate(part.getLastModifiedDate() != null ? part.getLastModifiedDate().toString() : null)
            .build();
    }

    public Part toEntity(PartRequest request) {
        if (request == null) {
            return null;
        }

        return Part.builder()
            .name(request.getName())
            .description(request.getDescription())
            .partNumber(request.getPartNumber())
            .category(request.getCategory())
            .cost(request.getCost())
            .unit(request.getUnit())
            .supplier(request.getSupplier())
            .minimumStock(request.getMinimumStock())
            .reorderQuantity(request.getReorderQuantity())
            .notes(request.getNotes())
            // `currentStock` and `status` are set in the service layer so business rules are centralized
            .currentStock(request.getCurrentStock() != null ? request.getCurrentStock() : 0)
            .build();
    }
}
