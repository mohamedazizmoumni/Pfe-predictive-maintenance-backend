package com.pfe.predictive.inventory;

import com.pfe.predictive.common.service.EmailService;
import com.pfe.predictive.inventory.dto.PartRequest;
import com.pfe.predictive.inventory.dto.PartResponse;
import com.pfe.predictive.inventory.dto.PartUpdateRequest;
import com.pfe.predictive.inventory.dto.LowStockAlertResponse;
import com.pfe.predictive.inventory.dto.ShortageForecastRequest;
import com.pfe.predictive.inventory.service.PartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.server.ResponseStatusException;
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
    private final EmailService emailService;

    @Value("${ml.internal-api-key}")
    private String internalApiKey;

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
                emailService.sendEmailToUsersByRoles(
                    List.of("STOCK_MANAGER"),
                    "Out of Stock: " + updatedPart.getName(),
                    buildShortageEmail(updatedPart, "is now out of stock")
                );
            } else if ("LOW_STOCK".equals(newStatus) && !"LOW_STOCK".equals(oldStatus) && !"OUT_OF_STOCK".equals(oldStatus)) {
                stockNotificationService.notifyLowStock(
                    updatedPart.getName(),
                    updatedPart.getCurrentStock(),
                    updatedPart.getMinimumStock(),
                    updatedPart.getReorderQuantity()
                );
                emailService.sendEmailToUsersByRoles(
                    List.of("STOCK_MANAGER"),
                    "Low Stock Alert: " + updatedPart.getName(),
                    buildShortageEmail(updatedPart, "has fallen to or below the minimum threshold")
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

    @PostMapping("/{id}/shortage-forecast")
    @Operation(summary = "Receive an AI shortage prediction from the ML service and notify the Stock Manager")
    public ResponseEntity<Void> receiveShortageForecast(
            @PathVariable Long id,
            @Valid @RequestBody ShortageForecastRequest request,
            @RequestHeader("X-Internal-Key") String internalKeyHeader) {

        if (!internalApiKey.equals(internalKeyHeader)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid internal key");
        }

        PartResponse part = partService.getPartById(id);
        log.info("Received AI shortage forecast for part {}: recommendedQuantity={}", part.getName(), request.getRecommendedQuantity());

        stockNotificationService.notifyAiShortagePrediction(
            part.getName(),
            request.getRecommendedQuantity(),
            request.getReason()
        );

        emailService.sendEmailToUsersByRoles(
            List.of("STOCK_MANAGER"),
            "AI Shortage Prediction: " + part.getName(),
            buildAiShortageEmail(part, request)
        );

        return ResponseEntity.accepted().build();
    }

    private String buildAiShortageEmail(PartResponse part, ShortageForecastRequest request) {
        String stockoutLine = request.getPredictedStockoutDate() != null
            ? "<p><strong>Predicted stockout date:</strong> " + request.getPredictedStockoutDate() + "</p>"
            : "";
        String confidenceLine = request.getConfidence() != null
            ? "<p><strong>Model confidence:</strong> " + Math.round(request.getConfidence() * 100) + "%</p>"
            : "";

        return "<div style=\"font-family:Arial,sans-serif;font-size:14px;color:#111827;\">"
            + "<h2 style=\"margin:0 0 12px 0;\">🤖 AI Shortage Prediction</h2>"
            + "<p><strong>Part:</strong> " + part.getName() + "</p>"
            + "<p><strong>Current stock:</strong> " + part.getCurrentStock() + "</p>"
            + "<p><strong>Minimum stock:</strong> " + part.getMinimumStock() + "</p>"
            + stockoutLine
            + "<p><strong>Recommended reorder quantity:</strong> " + request.getRecommendedQuantity() + "</p>"
            + "<p><strong>Reason:</strong> " + request.getReason() + "</p>"
            + confidenceLine
            + "<p style=\"margin-top:16px;color:#6b7280;font-size:12px;\">"
            + "This is an AI-generated recommendation for your review. No Reorder Request or purchase order "
            + "has been created automatically — please log in to the Predictive Maintenance Platform to create "
            + "one if you agree with this recommendation.</p>"
            + "</div>";
    }

    private String buildShortageEmail(PartResponse part, String reasonPhrase) {
        return "<div style=\"font-family:Arial,sans-serif;font-size:14px;color:#111827;\">"
            + "<h2 style=\"margin:0 0 12px 0;\">Stock Shortage Detected</h2>"
            + "<p><strong>Part:</strong> " + part.getName() + "</p>"
            + "<p><strong>Current stock:</strong> " + part.getCurrentStock() + "</p>"
            + "<p><strong>Minimum stock:</strong> " + part.getMinimumStock() + "</p>"
            + "<p><strong>Recommended reorder quantity:</strong> " + part.getReorderQuantity() + "</p>"
            + "<p><strong>Reason:</strong> Current stock (" + part.getCurrentStock() + ") " + reasonPhrase
            + " (" + part.getMinimumStock() + ").</p>"
            + "<p style=\"margin-top:16px;color:#6b7280;font-size:12px;\">Please log in to the Predictive Maintenance Platform to create a reorder request for this part.</p>"
            + "</div>";
    }
}
