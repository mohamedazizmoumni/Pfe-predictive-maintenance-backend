package com.pfe.predictive.inventory;

import com.pfe.predictive.inventory.dto.ReorderApprovalRequest;
import com.pfe.predictive.inventory.dto.ReorderRequestRequest;
import com.pfe.predictive.inventory.dto.ReorderRequestResponse;
import com.pfe.predictive.inventory.service.ReorderService;
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

@Slf4j
@RestController
@RequestMapping("/api/v1/inventory/reorders")
@RequiredArgsConstructor
@Tag(name = "Reorder Requests", description = "Reorder request management for low-stock parts")
public class ReorderController {
    
    private final ReorderService reorderService;
    
    @PostMapping
    @PreAuthorize("hasAnyRole('STOCK_MANAGER', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create reorder request", description = "Request reorder for a part")
    public ResponseEntity<ReorderRequestResponse> createReorderRequest(
            @Valid @RequestBody ReorderRequestRequest request,
            Authentication authentication) {
        
        String requestedBy = authentication.getName();
        log.info("Creating reorder request for part {} by {}", request.getPartId(), requestedBy);
        
        ReorderRequestResponse response = reorderService.requestReorder(request, requestedBy);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
 
    @GetMapping
    @PreAuthorize("hasAnyRole('STOCK_MANAGER', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get all reorders", description = "Retrieve all reorder requests")
    public ResponseEntity<java.util.Map<String, Object>> getAllReorders() {
        log.info("Fetching all reorder requests");
        List<ReorderRequestResponse> reorders = reorderService.getAllReorders();
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("content", reorders);
        response.put("totalElements", reorders.size());
        response.put("size", reorders.size());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('STOCK_MANAGER', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get pending reorders", description = "Retrieve all pending reorder requests")
    public ResponseEntity<List<ReorderRequestResponse>> getPendingReorders() {
        log.info("Fetching pending reorder requests");
        List<ReorderRequestResponse> reorders = reorderService.getPendingReorders();
        return ResponseEntity.ok(reorders);
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STOCK_MANAGER', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get reorder by ID", description = "Retrieve a specific reorder request")
    public ResponseEntity<ReorderRequestResponse> getReorderById(@PathVariable Long id) {
        log.info("Fetching reorder request: {}", id);
        ReorderRequestResponse reorder = reorderService.getReorderById(id);
        return ResponseEntity.ok(reorder);
    }
    
    @PutMapping("/{id}/approval")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Approve/reject reorder", description = "Approve or reject a reorder request")
    public ResponseEntity<ReorderRequestResponse> processReorderApproval(
            @PathVariable Long id,
            @Valid @RequestBody ReorderApprovalRequest request,
            Authentication authentication) {
        
        String approvedBy = authentication.getName();
        log.info("Processing reorder approval for {} by {}: {}", id, approvedBy, request.getApproved());
        
        ReorderRequestResponse response = reorderService.approveReorder(id, request, approvedBy);
        
        return ResponseEntity.ok(response);
    }
    
}
