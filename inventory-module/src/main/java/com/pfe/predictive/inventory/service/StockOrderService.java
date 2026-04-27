package com.pfe.predictive.inventory.service;

import com.pfe.predictive.inventory.dto.StockOrderRequest;
import com.pfe.predictive.inventory.dto.StockOrderReceiptRequest;
import com.pfe.predictive.inventory.dto.StockOrderResponse;
import com.pfe.predictive.inventory.entity.ReorderRequest;
import com.pfe.predictive.inventory.entity.StockOrder;
import com.pfe.predictive.inventory.entity.StockOrderStatus;
import com.pfe.predictive.inventory.mapper.StockOrderMapper;
import com.pfe.predictive.inventory.repository.ReorderRequestRepository;
import com.pfe.predictive.inventory.repository.StockOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StockOrderService {

    private final StockOrderRepository stockOrderRepository;
    private final ReorderRequestRepository reorderRequestRepository;
    private final StockOrderMapper stockOrderMapper;

    @Transactional(readOnly = true)
    public List<StockOrderResponse> getAllOrders() {
        return stockOrderRepository.findAll()
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

        StockOrder order = stockOrderMapper.toEntity(request, reorder, orderedBy);
        StockOrder saved = stockOrderRepository.save(order);
        return stockOrderMapper.toResponse(saved);
    }

    public StockOrderResponse markOrderDelivered(Long id, StockOrderReceiptRequest receipt) {
        StockOrder order = stockOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Stock order not found: " + id));

        order.setStatus(StockOrderStatus.DELIVERED);
        order.setDeliveredDate(receipt.getProofOfDelivery());
        order.setNotes(receipt.getNotes());

        StockOrder updated = stockOrderRepository.save(order);
        return stockOrderMapper.toResponse(updated);
    }
}
