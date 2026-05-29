package com.pfe.predictive.inventory;

import com.pfe.predictive.inventory.dto.PartRequest;
import com.pfe.predictive.inventory.dto.PartResponse;
import com.pfe.predictive.inventory.dto.PartUpdateRequest;
import com.pfe.predictive.inventory.dto.LowStockAlertResponse;
import com.pfe.predictive.inventory.service.PartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/inventory/parts")
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "Parts and inventory management")
public class InventoryController {
    
    private final PartService partService;
    private final StockNotificationService stockNotificationService;
    
    @GetMapping
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'STOCK_MANAGER', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get all parts")
    public ResponseEntity<List<PartResponse>> getAllParts() {
        log.info("Fetching all parts");
        List<PartResponse> parts = partService.getAllParts();
        return ResponseEntity.ok(parts);
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'STOCK_MANAGER', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get part by ID")
    public ResponseEntity<PartResponse> getPartById(@PathVariable Long id) {
        log.info("Fetching part: {}", id);
        PartResponse part = partService.getPartById(id);
        return ResponseEntity.ok(part);
    }
    
    @PostMapping
    @PreAuthorize("hasAnyRole('STOCK_MANAGER', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create new part")
    public ResponseEntity<PartResponse> createPart(@Valid @RequestBody PartRequest request) {
        log.info("Creating part: {}", request.getName());
        PartResponse part = partService.createPart(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(part);
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('STOCK_MANAGER', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update part")
    public ResponseEntity<PartResponse> updatePart(
            @PathVariable Long id,
            @Valid @RequestBody PartUpdateRequest request) {
        
        log.info("Updating part: {}", id);
        
        PartResponse oldPart = partService.getPartById(id);
        

        PartResponse updatedPart = partService.updatePart(id, request);
        
        if (request.getCurrentStock() != null) {
            String oldStatus = oldPart.getStatus();
            String newStatus = updatedPart.getStatus();
            
            if ("OUT_OF_STOCK".equals(newStatus) && !"OUT_OF_STOCK".equals(oldStatus)) {
                stockNotificationService.notifyOutOfStock(
                    updatedPart.getName(),
                    updatedPart.getReorderQuantity()
                );
            } else if ("LOW_STOCK".equals(newStatus) && !"LOW_STOCK".equals(oldStatus) && !"OUT_OF_STOCK".equals(oldStatus)) {
                stockNotificationService.notifyLowStock(
                    updatedPart.getName(),
                    updatedPart.getCurrentStock(),
                    updatedPart.getMinimumStock(),
                    updatedPart.getReorderQuantity()
                );
            }
        }
        
        return ResponseEntity.ok(updatedPart);
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Delete part")
    public ResponseEntity<Void> deletePart(@PathVariable Long id) {
        log.info("Deleting part: {}", id);
        partService.deletePart(id);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/{id}/image")
    @PreAuthorize("hasAnyRole('STOCK_MANAGER', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Upload part image")
    public ResponseEntity<PartResponse> uploadPartImage(
            @PathVariable Long id,
            @RequestParam("image") org.springframework.web.multipart.MultipartFile image) {
        
        log.info("Uploading image for part: {}", id);
        PartResponse part = partService.uploadPartImage(id, image);
        return ResponseEntity.ok(part);
    }
    
    @DeleteMapping("/{id}/image")
    @PreAuthorize("hasAnyRole('STOCK_MANAGER', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Delete part image")
    public ResponseEntity<PartResponse> deletePartImage(@PathVariable Long id) {
        log.info("Deleting image for part: {}", id);
        PartResponse part = partService.deletePartImage(id);
        return ResponseEntity.ok(part);
    }
    
    @GetMapping("/category/{category}")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'STOCK_MANAGER', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get parts by category")
    public ResponseEntity<List<PartResponse>> getPartsByCategory(@PathVariable String category) {
        log.info("Fetching parts for category: {}", category);
        List<PartResponse> parts = partService.getPartsByCategory(category);
        return ResponseEntity.ok(parts);
    }
    
    @GetMapping("/subcategory/{subCategory}")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'STOCK_MANAGER', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get parts by subcategory")
    public ResponseEntity<List<PartResponse>> getPartsBySubCategory(@PathVariable String subCategory) {
        log.info("Fetching parts for subcategory: {}", subCategory);
        List<PartResponse> parts = partService.getPartsBySubCategory(subCategory);
        return ResponseEntity.ok(parts);
    }
   
    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('STOCK_MANAGER', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get low stock parts")
    public ResponseEntity<List<LowStockAlertResponse>> getLowStockParts() {
        log.info("Fetching low stock parts");
        List<LowStockAlertResponse> parts = partService.getLowStockParts();
        return ResponseEntity.ok(parts);
    }
}
