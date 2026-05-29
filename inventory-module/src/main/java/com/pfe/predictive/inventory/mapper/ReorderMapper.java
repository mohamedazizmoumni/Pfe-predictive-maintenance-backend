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

        Part part = reorder.getPart();

        return ReorderRequestResponse.builder()
                .id(reorder.getId())
                .partId(part != null ? part.getId() : null)
                .partName(part != null ? part.getName() : null)
                .quantity(reorder.getQuantity())
                .approximateCost(calculateTotalCost(reorder))
                .reason(reorder.getReason())
                .status(reorder.getStatus() != null ? reorder.getStatus().toString() : null)
                .requestedBy(reorder.getRequestedBy())
                .requestedDate(reorder.getCreatedDate() != null ? reorder.getCreatedDate().toString() : null)
                .approvedBy(reorder.getApprovedBy())
                .approvedDate(reorder.getApprovedDate() != null ? reorder.getApprovedDate().toString() : null)
                .notes(reorder.getNotes())
                .build();
    }

    public ReorderRequest toEntity(
            ReorderRequestRequest request,
            Part part,
            String requestedBy
    ) {
        if (request == null || part == null) {
            throw new IllegalArgumentException("Request or part cannot be null");
        }

        return ReorderRequest.builder()
                .part(part)
                .quantity(request.getQuantity() != null ? request.getQuantity() : 1)
                .reason(request.getReason())
                .requestedBy(requestedBy)
                .notes(request.getNotes())
                .status(ReorderStatus.REQUESTED)
                .build();
    }

    private BigDecimal calculateTotalCost(ReorderRequest reorder) {
        if (reorder == null ||
            reorder.getPart() == null ||
            reorder.getQuantity() == null ||
            reorder.getPart().getCost() == null) {
            return BigDecimal.ZERO;
        }

        return reorder.getPart().getCost()
                .multiply(BigDecimal.valueOf(reorder.getQuantity()))
                .setScale(2, RoundingMode.HALF_UP);
    }
}