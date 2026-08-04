package com.pfe.predictive.inventory;

import com.pfe.predictive.inventory.dto.StockOrderReceiptRequest;
import com.pfe.predictive.inventory.dto.StockOrderRequest;
import com.pfe.predictive.inventory.dto.StockOrderResponse;
import com.pfe.predictive.inventory.service.StockOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Stock Order Controller
 * Handles stock orders created from approved reorder requests
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/inventory/orders")
@RequiredArgsConstructor
@Tag(name = "Stock Orders", description = "Stock order management and tracking")
public class StockOrderController {
    
    private final StockOrderService stockOrderService;
    
    /**
     * Get all stock orders
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('STOCK_MANAGER', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get all orders", description = "Retrieve all stock orders")
    public ResponseEntity<List<StockOrderResponse>> getAllOrders() {
        log.info("Fetching all stock orders");
        List<StockOrderResponse> orders = stockOrderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }
    
    /**
     * Get stock order by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STOCK_MANAGER', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get order by ID", description = "Retrieve a specific stock order")
    public ResponseEntity<StockOrderResponse> getOrderById(@PathVariable Long id) {
        log.info("Fetching stock order: {}", id);
        StockOrderResponse order = stockOrderService.getOrderById(id);
        return ResponseEntity.ok(order);
    }
    
    /**
     * Create a stock order from an approved reorder request
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('STOCK_MANAGER', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create stock order", description = "Create a stock order from an approved reorder request")
    public ResponseEntity<StockOrderResponse> createOrder(
            @Valid @RequestBody StockOrderRequest request,
            Authentication authentication) {
        
        String orderedBy = authentication.getName();
        log.info("Creating stock order for reorder request {} by {}", request.getReorderRequestId(), orderedBy);
        
        StockOrderResponse response = stockOrderService.createOrder(request.getReorderRequestId(), request, orderedBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Mark order as shipped
     */
    @PutMapping("/{id}/ship")
    @PreAuthorize("hasAnyRole('STOCK_MANAGER', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Mark order as shipped", description = "Transition a pending stock order to shipped")
    public ResponseEntity<StockOrderResponse> markOrderShipped(@PathVariable Long id) {
        log.info("Marking stock order {} as shipped", id);

        StockOrderResponse response = stockOrderService.markOrderShipped(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Mark order as delivered
     */
    @PutMapping("/{id}/deliver")
    @PreAuthorize("hasAnyRole('STOCK_MANAGER', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Mark order as delivered", description = "Mark order as delivered with proof of delivery")
    public ResponseEntity<StockOrderResponse> markOrderDelivered(
            @PathVariable Long id,
            @Valid @RequestBody StockOrderReceiptRequest request) {
        
        log.info("Marking stock order {} as delivered", id);
        
        StockOrderResponse response = stockOrderService.markOrderDelivered(id, request);
        return ResponseEntity.ok(response);
    }
}
