package com.pfe.predictive.notification.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pfe.predictive.core.entity.RiskLevel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Notification entity for alerting users to HIGH and CRITICAL machine predictions.
 * Notifications are poll-based (REST) rather than push-based (WebSocket/SSE).
 */
@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notification_machine_time", columnList = "machine_id, created_at DESC"),
    @Index(name = "idx_notification_is_read", columnList = "is_read"),
    @Index(name = "idx_notification_created_at", columnList = "created_at DESC")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Reference to the machine that triggered the notification.
     * Null for stock/inventory notifications, which are not machine-specific.
     */
    @Column(name = "machine_id")
    private Long machineId;

    /**
     * Notification title (short message).
     */
    @Column(name = "title", length = 255, nullable = false)
    private String title;

    /**
     * Notification body (detailed message with RUL and risk level).
     */
    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    /**
     * Risk level that triggered the notification.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false)
    private RiskLevel riskLevel;

    /**
     * Reference to the prediction record that triggered this notification.
     */
    @Column(name = "prediction_record_id")
    private Long predictionRecordId;

    /**
     * Comma-separated list of target roles (e.g., "TECHNICIAN,MANAGER,ADMIN").
     * Allows filtering notifications by user role.
     */
    @Column(name = "target_roles", length = 100)
    private String targetRoles;

    /**
     * Whether the notification has been read by the user.
     * Defaults to false.
     */
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    /**
     * Timestamp when the notification was created.
     * Auto-managed, not updatable.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
