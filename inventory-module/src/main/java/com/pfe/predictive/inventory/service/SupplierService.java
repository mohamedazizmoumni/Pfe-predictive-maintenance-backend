package com.pfe.predictive.inventory.service;

import com.pfe.predictive.inventory.dto.SupplierRequest;
import com.pfe.predictive.inventory.dto.SupplierResponse;
import com.pfe.predictive.inventory.dto.SupplierScorecardResponse;
import com.pfe.predictive.inventory.entity.StockOrder;
import com.pfe.predictive.inventory.entity.StockOrderStatus;
import com.pfe.predictive.inventory.entity.Supplier;
import com.pfe.predictive.inventory.repository.StockOrderRepository;
import com.pfe.predictive.inventory.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final StockOrderRepository stockOrderRepository;

    public SupplierResponse create(SupplierRequest request) {
        Supplier supplier = Supplier.builder()
                .name(request.getName())
                .contactName(request.getContactName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .leadTimeDays(request.getLeadTimeDays())
                .notes(request.getNotes())
                .active(request.getActive() == null || request.getActive())
                .build();
        return toResponse(supplierRepository.save(supplier));
    }

    public SupplierResponse update(Long id, SupplierRequest request) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + id));

        supplier.setName(request.getName());
        if (request.getContactName() != null) supplier.setContactName(request.getContactName());
        if (request.getEmail() != null) supplier.setEmail(request.getEmail());
        if (request.getPhone() != null) supplier.setPhone(request.getPhone());
        if (request.getAddress() != null) supplier.setAddress(request.getAddress());
        if (request.getLeadTimeDays() != null) supplier.setLeadTimeDays(request.getLeadTimeDays());
        if (request.getNotes() != null) supplier.setNotes(request.getNotes());
        if (request.getActive() != null) supplier.setActive(request.getActive());

        return toResponse(supplierRepository.save(supplier));
    }

    public void delete(Long id) {
        if (!supplierRepository.existsById(id)) {
            throw new IllegalArgumentException("Supplier not found: " + id);
        }
        // Soft delete — a supplier may already be referenced by historical
        // stock orders, and deleting the row would orphan that reporting data.
        Supplier supplier = supplierRepository.findById(id).orElseThrow();
        supplier.setActive(false);
        supplierRepository.save(supplier);
    }

    @Transactional(readOnly = true)
    public List<SupplierResponse> getAll(boolean activeOnly) {
        List<Supplier> suppliers = activeOnly ? supplierRepository.findByActiveTrue() : supplierRepository.findAll();
        return suppliers.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public SupplierResponse getById(Long id) {
        return toResponse(supplierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + id)));
    }

    @Transactional(readOnly = true)
    public SupplierScorecardResponse getScorecard(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + id));

        List<StockOrder> orders = stockOrderRepository.findBySupplierId(id);
        long totalOrders = orders.size();

        List<StockOrder> delivered = orders.stream()
                .filter(o -> o.getStatus() == StockOrderStatus.DELIVERED)
                .toList();

        BigDecimal totalSpend = orders.stream()
                .map(o -> o.getCost() != null ? o.getCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Double> leadTimesDays = delivered.stream()
                .map(this::actualLeadTimeDays)
                .filter(java.util.Objects::nonNull)
                .toList();

        Double avgActualLeadTimeDays = leadTimesDays.isEmpty() ? null
                : leadTimesDays.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        Double onTimeRate = null;
        if (supplier.getLeadTimeDays() != null && !leadTimesDays.isEmpty()) {
            long onTime = leadTimesDays.stream().filter(days -> days <= supplier.getLeadTimeDays()).count();
            onTimeRate = (double) onTime / leadTimesDays.size();
        }

        return SupplierScorecardResponse.builder()
                .supplierId(supplier.getId())
                .supplierName(supplier.getName())
                .totalOrders(totalOrders)
                .deliveredOrders(delivered.size())
                .totalSpend(totalSpend)
                .avgActualLeadTimeDays(avgActualLeadTimeDays)
                .onTimeDeliveryRate(onTimeRate)
                .build();
    }

    /** Parses orderedDate (real timestamp) vs deliveredDate (string) defensively — bad/legacy data just gets skipped, not thrown. */
    private Double actualLeadTimeDays(StockOrder order) {
        if (order.getOrderedDate() == null || order.getDeliveredDate() == null) {
            return null;
        }
        try {
            LocalDateTime delivered = LocalDateTime.parse(order.getDeliveredDate());
            return Duration.between(order.getOrderedDate(), delivered).toMinutes() / (60.0 * 24.0);
        } catch (DateTimeParseException ex) {
            log.debug("Stock order {} has a non-parseable deliveredDate '{}' — skipping in scorecard", order.getId(), order.getDeliveredDate());
            return null;
        }
    }

    private SupplierResponse toResponse(Supplier supplier) {
        return SupplierResponse.builder()
                .id(supplier.getId())
                .name(supplier.getName())
                .contactName(supplier.getContactName())
                .email(supplier.getEmail())
                .phone(supplier.getPhone())
                .address(supplier.getAddress())
                .leadTimeDays(supplier.getLeadTimeDays())
                .notes(supplier.getNotes())
                .active(supplier.isActive())
                .createdDate(supplier.getCreatedDate())
                .build();
    }
}
