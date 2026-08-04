package com.pfe.predictive.inventory.service;

import com.pfe.predictive.inventory.dto.StockOrderRequest;
import com.pfe.predictive.inventory.dto.StockOrderReceiptRequest;
import com.pfe.predictive.inventory.dto.StockOrderResponse;
import com.pfe.predictive.inventory.entity.ReorderRequest;
import com.pfe.predictive.inventory.entity.ReorderStatus;
import com.pfe.predictive.inventory.entity.StockOrder;
import com.pfe.predictive.inventory.entity.StockOrderStatus;
import com.pfe.predictive.inventory.entity.Supplier;
import com.pfe.predictive.inventory.mapper.StockOrderMapper;
import com.pfe.predictive.inventory.repository.ReorderRequestRepository;
import com.pfe.predictive.inventory.repository.StockOrderRepository;
import com.pfe.predictive.inventory.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StockOrderService {

    // getAllOrders() has no client-driven paging yet - cap at a generous
    // size (most recent first) instead of loading the entire order history.
    private static final int LIST_CAP = 300;

    private final StockOrderRepository stockOrderRepository;
    private final ReorderRequestRepository reorderRequestRepository;
    private final StockOrderMapper stockOrderMapper;
    private final PartService partService;
    private final SupplierRepository supplierRepository;

    @Transactional(readOnly = true)
    public List<StockOrderResponse> getAllOrders() {
        return stockOrderRepository.findAll(PageRequest.of(0, LIST_CAP, Sort.by(Sort.Direction.DESC, "orderedDate")))
                .getContent()
                .stream()
                .map(stockOrderMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public StockOrderResponse getOrderById(Long id) {
        StockOrder order = stockOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Stock order not found: " + id));
        return stockOrderMapper.toResponse(order);
    }

    public StockOrderResponse createOrder(Long reorderRequestId, StockOrderRequest request, String orderedBy) {
        ReorderRequest reorder = reorderRequestRepository.findById(reorderRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Reorder request not found: " + reorderRequestId));

        if (reorder.getStatus() != ReorderStatus.APPROVED) {
            throw new IllegalStateException(
                "Reorder request " + reorderRequestId + " cannot be ordered — current status is " + reorder.getStatus());
        }

        Supplier supplier = request.getSupplierId() != null
                ? supplierRepository.findById(request.getSupplierId())
                    .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + request.getSupplierId()))
                : null;

        StockOrder order = stockOrderMapper.toEntity(request, reorder, orderedBy, supplier);
        StockOrder saved = stockOrderRepository.save(order);

        reorder.setStatus(ReorderStatus.ORDERED);
        reorderRequestRepository.save(reorder);

        return stockOrderMapper.toResponse(saved);
    }

    public StockOrderResponse markOrderShipped(Long id) {
        StockOrder order = stockOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Stock order not found: " + id));

        if (order.getStatus() != StockOrderStatus.PENDING) {
            throw new IllegalStateException(
                "Stock order " + id + " cannot be shipped — current status is " + order.getStatus());
        }

        order.setStatus(StockOrderStatus.SHIPPED);

        StockOrder updated = stockOrderRepository.save(order);
        return stockOrderMapper.toResponse(updated);
    }

    public StockOrderResponse markOrderDelivered(Long id, StockOrderReceiptRequest receipt) {
        StockOrder order = stockOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Stock order not found: " + id));

        if (order.getStatus() != StockOrderStatus.SHIPPED) {
            throw new IllegalStateException(
                "Stock order " + id + " cannot be marked delivered — current status is " + order.getStatus());
        }

        order.setStatus(StockOrderStatus.DELIVERED);
        // Bug fix: this previously stored the proof-of-delivery text in the
        // date field and never populated proofOfDelivery at all — silently
        // corrupting delivery dates and losing the proof reference.
        order.setDeliveredDate(java.time.LocalDateTime.now().toString());
        order.setProofOfDelivery(receipt.getProofOfDelivery());
        order.setNotes(receipt.getNotes());

        StockOrder updated = stockOrderRepository.save(order);

        int quantityReceived = receipt.getQuantityReceived() != null
                ? receipt.getQuantityReceived()
                : order.getQuantity();
        partService.updateStockAfterReceipt(order.getPart().getId(), quantityReceived);

        return stockOrderMapper.toResponse(updated);
    }
}
