package com.pfe.predictive.inventory;

import com.pfe.predictive.audit.service.AuditEventService;
import com.pfe.predictive.inventory.dto.PartReservationRequest;
import com.pfe.predictive.inventory.dto.PartReservationResponse;
import com.pfe.predictive.inventory.service.PartReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Reserves stock against a specific job before it's consumed — folds
 * "reserve then use" into the existing Part/StockOrder inventory model
 * without touching currentStock until consume() actually happens.
 */
@RestController
@RequestMapping("/api/v1/inventory/reservations")
@RequiredArgsConstructor
public class PartReservationController {

    private final PartReservationService reservationService;
    private final AuditEventService auditEventService;

    @PostMapping
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'MANAGER', 'STOCK_MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<PartReservationResponse> reserve(@Valid @RequestBody PartReservationRequest request,
                                                             Authentication authentication) {
        PartReservationResponse response = reservationService.reserve(request, authentication.getName());
        auditEventService.record(authentication.getName(), "PART_RESERVED", "Part", response.getPartId(),
                "quantity=" + response.getQuantityReserved() + ", maintenanceId=" + response.getMaintenanceId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}/release")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'MANAGER', 'STOCK_MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<PartReservationResponse> release(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.release(id));
    }

    @PutMapping("/{id}/consume")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'MANAGER', 'STOCK_MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<PartReservationResponse> consume(
            @PathVariable Long id,
            @RequestParam(required = false) Integer quantityUsed,
            Authentication authentication) {
        PartReservationResponse response = reservationService.consume(id, quantityUsed);
        auditEventService.record(authentication.getName(), "PART_RESERVATION_CONSUMED", "Part", response.getPartId(),
                "quantityConsumed=" + response.getQuantityConsumed() + " of " + response.getQuantityReserved() + " reserved");
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'MANAGER', 'STOCK_MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<PartReservationResponse>> byPart(@RequestParam Long partId) {
        return ResponseEntity.ok(reservationService.byPart(partId));
    }

    @GetMapping("/by-maintenance/{maintenanceId}")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'MANAGER', 'STOCK_MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<PartReservationResponse>> byMaintenance(@PathVariable Long maintenanceId) {
        return ResponseEntity.ok(reservationService.byMaintenance(maintenanceId));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('STOCK_MANAGER', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<Long, Integer>> summary() {
        return ResponseEntity.ok(reservationService.getReservedQuantitiesByPart());
    }
}
