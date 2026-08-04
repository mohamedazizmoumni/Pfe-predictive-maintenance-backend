package com.pfe.predictive.inventory.mapper;

import com.pfe.predictive.inventory.dto.StockOrderRequest;
import com.pfe.predictive.inventory.dto.StockOrderResponse;
import com.pfe.predictive.inventory.entity.ReorderRequest;
import com.pfe.predictive.inventory.entity.StockOrder;
import com.pfe.predictive.inventory.entity.StockOrderStatus;
import com.pfe.predictive.inventory.entity.Supplier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class StockOrderMapper {

    public StockOrderResponse toResponse(StockOrder order) {
        if (order == null) {
            return null;
        }

        return StockOrderResponse.builder()
            .id(order.getId())
            .reorderRequestId(order.getReorderRequest().getId())
            .partId(order.getPart().getId())
            .partName(order.getPart().getName())
            .quantity(order.getQuantity())
            .cost(order.getCost())
            .status(order.getStatus().toString())
            .supplierId(order.getSupplier() != null ? order.getSupplier().getId() : null)
            .supplierName(order.getSupplier() != null ? order.getSupplier().getName() : null)
            .supplierPurchaseOrder(order.getSupplierPurchaseOrder())
            .orderedDate(order.getOrderedDate() != null ? order.getOrderedDate().toString() : null)
            .expectedDeliveryDate(order.getExpectedDeliveryDate())
            .deliveredDate(order.getDeliveredDate())
            .orderedBy(order.getOrderedBy())
            .notes(order.getNotes())
            .build();
    }

    public StockOrder toEntity(StockOrderRequest request, ReorderRequest reorder, String orderedBy) {
        return toEntity(request, reorder, orderedBy, null);
    }

    public StockOrder toEntity(StockOrderRequest request, ReorderRequest reorder, String orderedBy, Supplier supplier) {
        if (request == null || reorder == null) {
            return null;
        }

        return StockOrder.builder()
            .reorderRequest(reorder)
            .part(reorder.getPart())
            .quantity(reorder.getQuantity())
            .cost(calculateTotalCost(reorder))
            .status(StockOrderStatus.PENDING)
            .supplier(supplier)
            .supplierPurchaseOrder(request.getSupplierPurchaseOrder())
            .expectedDeliveryDate(request.getExpectedDeliveryDate())
            .orderedBy(orderedBy)
            .notes(request.getNotes())
            .build();
    }

    private BigDecimal calculateTotalCost(ReorderRequest reorder) {
        if (reorder == null || reorder.getQuantity() == null || reorder.getPart() == null || reorder.getPart().getCost() == null) {
            return null;
        }

        BigDecimal unitCost = reorder.getPart().getCost();
        BigDecimal quantity = BigDecimal.valueOf(reorder.getQuantity());
        return unitCost.multiply(quantity).setScale(4, RoundingMode.HALF_UP);
    }
}
