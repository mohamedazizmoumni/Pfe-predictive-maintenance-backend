package com.pfe.predictive.inventory.mapper;

import com.pfe.predictive.inventory.dto.ReorderRequestRequest;
import com.pfe.predictive.inventory.dto.ReorderRequestResponse;
import com.pfe.predictive.inventory.entity.Part;
import com.pfe.predictive.inventory.entity.ReorderRequest;
import com.pfe.predictive.inventory.entity.ReorderStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class ReorderMapper {

    public ReorderRequestResponse toResponse(ReorderRequest reorder) {
        if (reorder == null) {
            return null;
        }

        return ReorderRequestResponse.builder()
            .id(reorder.getId())
            .partId(reorder.getPart().getId())
            .partName(reorder.getPart().getName())
            .quantity(reorder.getQuantity())
            .approximateCost(calculateTotalCost(reorder))
            .reason(reorder.getReason())
            .status(reorder.getStatus().toString())
            .requestedBy(reorder.getRequestedBy())
            .requestedDate(reorder.getCreatedDate() != null ? reorder.getCreatedDate().toString() : null)
            .approvedBy(reorder.getApprovedBy())
            .approvedDate(reorder.getApprovedDate() != null ? reorder.getApprovedDate().toString() : null)
            .notes(reorder.getNotes())
            .build();
    }

    public ReorderRequest toEntity(ReorderRequestRequest request, Part part, String requestedBy) {
        if (request == null || part == null) {
            return null;
        }

        return ReorderRequest.builder()
            .part(part)
            .quantity(request.getQuantity())
            .reason(request.getReason())
            .requestedBy(requestedBy)
            .notes(request.getNotes())
            .status(ReorderStatus.REQUESTED)
            .build();
    }

    private BigDecimal calculateTotalCost(ReorderRequest reorder) {
        if (reorder.getPart().getCost() == null) {
            return null;
        }

        BigDecimal unitCost = reorder.getPart().getCost();
        BigDecimal total = unitCost.multiply(BigDecimal.valueOf(reorder.getQuantity()));
        return total.setScale(2, RoundingMode.HALF_UP);
    }
}
