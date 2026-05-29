package com.pfe.predictive.inventory;

import com.pfe.predictive.inventory.entity.Part;
import com.pfe.predictive.inventory.entity.ReorderRequest;
import com.pfe.predictive.notification.entity.Notification;
import com.pfe.predictive.notification.repository.NotificationRepository;
import com.pfe.predictive.core.entity.RiskLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for creating stock-related notifications.
 * Automatically generates notifications for inventory events.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockNotificationService {
    
    private final NotificationRepository notificationRepository;
    
    /**
     * Create notification when part stock is low
     */
    @Transactional
    public void notifyLowStock(String partName, Integer currentStock, Integer minimumStock, Integer reorderQuantity) {
        try {
            Notification notification = Notification.builder()
                .machineId(null) // Stock notifications are not machine-specific
                .title("⚠️ Low Stock Alert: " + partName)
                .body(String.format(
                    "Part '%s' is running low. Current stock: %d, Minimum: %d. Reorder quantity: %d",
                    partName,
                    currentStock,
                    minimumStock,
                    reorderQuantity
                ))
                .riskLevel(RiskLevel.MEDIUM)
                .targetRoles("STOCK_MANAGER,MANAGER,ADMIN")
                .isRead(false)
                .build();
            
            notificationRepository.save(notification);
            log.info("Created low stock notification for part: {}", partName);
        } catch (Exception e) {
            log.error("Failed to create low stock notification for part: {}", partName, e);
        }
    }
    
    /**
     * Create notification when part is out of stock
     */
    @Transactional
    public void notifyOutOfStock(String partName, Integer reorderQuantity) {
        try {
            Notification notification = Notification.builder()
                .machineId(null) // Stock notifications are not machine-specific
                .title("🚨 OUT OF STOCK: " + partName)
                .body(String.format(
                    "URGENT: Part '%s' is out of stock! Immediate reorder required. Reorder quantity: %d",
                    partName,
                    reorderQuantity
                ))
                .riskLevel(RiskLevel.HIGH)
                .targetRoles("STOCK_MANAGER,MANAGER,ADMIN")
                .isRead(false)
                .build();
            
            notificationRepository.save(notification);
            log.info("Created out of stock notification for part: {}", partName);
        } catch (Exception e) {
            log.error("Failed to create out of stock notification for part: {}", partName, e);
        }
    }
    
    /**
     * Create notification when reorder request is created
     */
    @Transactional
    public void notifyReorderCreated(Long reorderId, String partName, Integer quantity, String requestedBy) {
        try {
            Notification notification = Notification.builder()
                .machineId(null) // Stock notifications are not machine-specific
                .title("📦 New Reorder Request: " + partName)
                .body(String.format(
                    "Reorder request created by %s for part '%s'. Quantity: %d. Status: PENDING APPROVAL",
                    requestedBy,
                    partName,
                    quantity
                ))
                .riskLevel(RiskLevel.LOW)
                .targetRoles("MANAGER,ADMIN")
                .isRead(false)
                .build();
            
            notificationRepository.save(notification);
            log.info("Created reorder request notification for part: {}", partName);
        } catch (Exception e) {
            log.error("Failed to create reorder notification", e);
        }
    }
    
    /**
     * Create notification when reorder is approved
     */
    @Transactional
    public void notifyReorderApproved(String partName, Integer quantity, String approvedBy) {
        try {
            Notification notification = Notification.builder()
                .machineId(null) // Stock notifications are not machine-specific
                .title("✅ Reorder Approved: " + partName)
                .body(String.format(
                    "Reorder request approved by %s for part '%s'. Quantity: %d. You can now create a stock order.",
                    approvedBy,
                    partName,
                    quantity
                ))
                .riskLevel(RiskLevel.LOW)
                .targetRoles("STOCK_MANAGER,MANAGER")
                .isRead(false)
                .build();
            
            notificationRepository.save(notification);
            log.info("Created reorder approved notification for part: {}", partName);
        } catch (Exception e) {
            log.error("Failed to create reorder approved notification", e);
        }
    }
    
    /**
     * Create notification when reorder is rejected
     */
    @Transactional
    public void notifyReorderRejected(String partName, String rejectedBy, String reason) {
        try {
            Notification notification = Notification.builder()
                .machineId(null) // Stock notifications are not machine-specific
                .title("❌ Reorder Rejected: " + partName)
                .body(String.format(
                    "Reorder request rejected by %s for part '%s'. Reason: %s",
                    rejectedBy,
                    partName,
                    reason != null ? reason : "No reason provided"
                ))
                .riskLevel(RiskLevel.LOW)
                .targetRoles("STOCK_MANAGER")
                .isRead(false)
                .build();
            
            notificationRepository.save(notification);
            log.info("Created reorder rejected notification for part: {}", partName);
        } catch (Exception e) {
            log.error("Failed to create reorder rejected notification", e);
        }
    }
}
