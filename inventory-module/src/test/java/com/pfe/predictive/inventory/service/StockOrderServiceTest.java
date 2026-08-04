package com.pfe.predictive.inventory.service;

import com.pfe.predictive.inventory.dto.StockOrderReceiptRequest;
import com.pfe.predictive.inventory.dto.StockOrderRequest;
import com.pfe.predictive.inventory.dto.StockOrderResponse;
import com.pfe.predictive.inventory.entity.Part;
import com.pfe.predictive.inventory.entity.ReorderRequest;
import com.pfe.predictive.inventory.entity.ReorderStatus;
import com.pfe.predictive.inventory.entity.StockOrder;
import com.pfe.predictive.inventory.entity.StockOrderStatus;
import com.pfe.predictive.inventory.mapper.StockOrderMapper;
import com.pfe.predictive.inventory.repository.ReorderRequestRepository;
import com.pfe.predictive.inventory.repository.StockOrderRepository;
import com.pfe.predictive.inventory.repository.SupplierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the purchase-order lifecycle: a reorder must be APPROVED before an
 * order can be placed (PENDING), then PENDING -> SHIPPED -> DELIVERED, with
 * delivery feeding the received quantity back into PartService's stock.
 */
@ExtendWith(MockitoExtension.class)
class StockOrderServiceTest {

    @Mock
    private StockOrderRepository stockOrderRepository;

    @Mock
    private ReorderRequestRepository reorderRequestRepository;

    @Mock
    private PartService partService;

    @Mock
    private SupplierRepository supplierRepository;

    private StockOrderService service;

    @BeforeEach
    void setUp() {
        service = new StockOrderService(stockOrderRepository, reorderRequestRepository, new StockOrderMapper(), partService, supplierRepository);
    }

    private Part part() {
        return Part.builder().id(5L).name("Bearing").cost(new java.math.BigDecimal("10.00")).build();
    }

    private ReorderRequest reorder(ReorderStatus status) {
        return ReorderRequest.builder().id(1L).part(part()).quantity(20).status(status).build();
    }

    private StockOrder order(StockOrderStatus status) {
        return StockOrder.builder()
                .id(100L)
                .reorderRequest(reorder(ReorderStatus.ORDERED))
                .part(part())
                .quantity(20)
                .status(status)
                .build();
    }

    // ------------------------------------------------------------------
    // createOrder: reorder must be APPROVED
    // ------------------------------------------------------------------

    @Test
    void createOrderRejectsNonApprovedReorder() {
        when(reorderRequestRepository.findById(1L)).thenReturn(Optional.of(reorder(ReorderStatus.REQUESTED)));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.createOrder(1L, StockOrderRequest.builder().build(), "stock.mgr"));

        assertEquals(true, ex.getMessage().contains("cannot be ordered"));
        verify(stockOrderRepository, never()).save(any());
    }

    @Test
    void createOrderMovesReorderToOrderedAndCreatesPendingOrder() {
        ReorderRequest reorder = reorder(ReorderStatus.APPROVED);
        when(reorderRequestRepository.findById(1L)).thenReturn(Optional.of(reorder));
        when(stockOrderRepository.save(any(StockOrder.class))).thenAnswer(inv -> {
            StockOrder o = inv.getArgument(0);
            o.setId(100L);
            return o;
        });

        StockOrderResponse response = service.createOrder(1L,
                StockOrderRequest.builder().supplierPurchaseOrder("PO-1").build(), "stock.mgr");

        assertEquals(StockOrderStatus.PENDING.toString(), response.getStatus());
        assertEquals(ReorderStatus.ORDERED, reorder.getStatus());
        verify(reorderRequestRepository).save(reorder);
    }

    // ------------------------------------------------------------------
    // markOrderShipped: PENDING -> SHIPPED
    // ------------------------------------------------------------------

    @Test
    void markOrderShippedRejectsNonPendingOrder() {
        when(stockOrderRepository.findById(100L)).thenReturn(Optional.of(order(StockOrderStatus.SHIPPED)));

        assertThrows(IllegalStateException.class, () -> service.markOrderShipped(100L));
        verify(stockOrderRepository, never()).save(any());
    }

    @Test
    void markOrderShippedSucceedsFromPending() {
        StockOrder order = order(StockOrderStatus.PENDING);
        when(stockOrderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(stockOrderRepository.save(any(StockOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        StockOrderResponse response = service.markOrderShipped(100L);

        assertEquals(StockOrderStatus.SHIPPED.toString(), response.getStatus());
    }

    // ------------------------------------------------------------------
    // markOrderDelivered: SHIPPED -> DELIVERED, feeds PartService
    // ------------------------------------------------------------------

    @Test
    void markOrderDeliveredRejectsNonShippedOrder() {
        when(stockOrderRepository.findById(100L)).thenReturn(Optional.of(order(StockOrderStatus.PENDING)));

        assertThrows(IllegalStateException.class,
                () -> service.markOrderDelivered(100L, StockOrderReceiptRequest.builder().build()));
        verify(partService, never()).updateStockAfterReceipt(any(), any());
    }

    @Test
    void markOrderDeliveredUsesExplicitReceivedQuantityWhenProvided() {
        StockOrder order = order(StockOrderStatus.SHIPPED);
        when(stockOrderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(stockOrderRepository.save(any(StockOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        StockOrderReceiptRequest receipt = StockOrderReceiptRequest.builder()
                .quantityReceived(15)
                .notes("Partial shipment, 5 units short")
                .build();

        StockOrderResponse response = service.markOrderDelivered(100L, receipt);

        assertEquals(StockOrderStatus.DELIVERED.toString(), response.getStatus());
        verify(partService).updateStockAfterReceipt(5L, 15);
    }

    @Test
    void markOrderDeliveredFallsBackToOrderedQuantityWhenReceivedQuantityOmitted() {
        StockOrder order = order(StockOrderStatus.SHIPPED);
        when(stockOrderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(stockOrderRepository.save(any(StockOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        service.markOrderDelivered(100L, StockOrderReceiptRequest.builder().build());

        // order.quantity is 20 in the fixture, with no explicit quantityReceived
        verify(partService).updateStockAfterReceipt(5L, 20);
    }

    @Test
    void getOrderByIdThrowsWhenMissing() {
        when(stockOrderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.getOrderById(999L));
    }
}
