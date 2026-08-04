package com.pfe.predictive.notification.controller;

import com.pfe.predictive.notification.entity.Notification;
import com.pfe.predictive.notification.repository.NotificationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API endpoints for managing notifications.
 * Notifications are triggered by HIGH and CRITICAL machine risk predictions.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Machine risk notifications management")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    /**
     * Get all notifications, optionally filtered by role.
     * 
     * @param role optional role filter (e.g., "MANAGER")
     * @return list of notifications
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('TECHNICIAN','MANAGER','ADMIN','DATA_SCIENTIST','SUPER_ADMIN','STOCK_MANAGER','FINANCE_MANAGER')")
    @Operation(summary = "Get notifications", description = "Retrieve notifications, optionally filtered by role")
    public ResponseEntity<List<Notification>> getNotifications(
            @RequestParam(required = false) String role) {

        try {
            List<Notification> notifications;

            if (role != null && !role.isBlank()) {
                notifications = notificationRepository
                        .findByTargetRolesContainingOrderByCreatedAtDesc(role, PageRequest.of(0, 50))
                        .getContent();
            } else {
                notifications = notificationRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 50))
                        .getContent();
            }

            // Return empty list instead of null
            if (notifications == null) {
                notifications = new java.util.ArrayList<>();
            }

            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            log.error("Error fetching notifications for role: {}", role, e);
            // Return empty list on error instead of 500
            return ResponseEntity.ok(new java.util.ArrayList<>());
        }
    }

    /**
     * Get count of unread notifications, optionally scoped to a role.
     *
     * Must mirror getNotifications' role filter exactly - the header bell's
     * badge count and its panel list come from these two endpoints
     * separately, and previously used different filters (this one was
     * unfiltered while the list was role-scoped), so the badge could show a
     * nonzero count while the panel opened empty.
     *
     * @param role optional role filter (e.g., "MANAGER") - same param as getNotifications
     * @return count of unread notifications
     */
    @GetMapping("/unread-count")
    @PreAuthorize("hasAnyRole('TECHNICIAN','MANAGER','ADMIN','DATA_SCIENTIST','SUPER_ADMIN','STOCK_MANAGER','FINANCE_MANAGER')")
    @Operation(summary = "Get unread notification count", description = "Returns the number of unread notifications, optionally filtered by role")
    public ResponseEntity<Map<String, Object>> getUnreadCount(
            @RequestParam(required = false) String role) {
        try {
            long count = (role != null && !role.isBlank())
                    ? notificationRepository.countByIsReadFalseAndTargetRolesContaining(role)
                    : notificationRepository.countByIsReadFalse();
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            log.error("Error counting unread notifications for role: {}", role, e);
            // Return 0 on error instead of 500
            return ResponseEntity.ok(Map.of("count", 0));
        }
    }

    /**
     * Mark a notification as read.
     * 
     * @param id notification ID
     * @return 200 OK on success, 404 if not found
     */
    @PutMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('TECHNICIAN','MANAGER','ADMIN','DATA_SCIENTIST','SUPER_ADMIN','STOCK_MANAGER','FINANCE_MANAGER')")
    @Operation(summary = "Mark notification as read", description = "Set isRead flag to true for a notification")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        return notificationRepository.findById(id)
                .map(notification -> {
                    notification.setIsRead(true);
                    notificationRepository.save(notification);
                    log.debug("event=notification_marked_read id={}", id);
                    return ResponseEntity.ok((Object) Map.of("message", "Notification marked as read"));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Notification not found")));
    }

    /**
     * Mark all unread notifications as read.
     * Restricted to MANAGER and above.
     * 
     * @return count of updated notifications
     */
    @PutMapping("/read-all")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN','SUPER_ADMIN','STOCK_MANAGER','FINANCE_MANAGER')")
    @Operation(summary = "Mark all notifications as read", description = "Set isRead flag to true for all unread notifications")
    public ResponseEntity<Map<String, Object>> markAllAsRead() {
        List<Notification> unreadNotifications = notificationRepository.findByIsReadFalseOrderByCreatedAtDesc();
        
        unreadNotifications.forEach(notification -> notification.setIsRead(true));
        notificationRepository.saveAll(unreadNotifications);

        log.info("event=all_notifications_marked_read count={}", unreadNotifications.size());

        return ResponseEntity.ok(Map.of("updated", unreadNotifications.size()));
    }

    /**
     * Get all notifications for a specific machine.
     * 
     * @param machineId ID of the machine
     * @return list of notifications for the machine
     */
    @GetMapping("/machine/{machineId}")
    @PreAuthorize("hasAnyRole('TECHNICIAN','MANAGER','ADMIN','DATA_SCIENTIST','SUPER_ADMIN','STOCK_MANAGER','FINANCE_MANAGER')")
    @Operation(summary = "Get machine notifications", description = "Retrieve all notifications for a specific machine")
    public ResponseEntity<List<Notification>> getMachineNotifications(@PathVariable Long machineId) {
        List<Notification> notifications = notificationRepository
                .findByMachineIdOrderByCreatedAtDesc(machineId, PageRequest.of(0, 100))
                .getContent();
        return ResponseEntity.ok(notifications);
    }
}
