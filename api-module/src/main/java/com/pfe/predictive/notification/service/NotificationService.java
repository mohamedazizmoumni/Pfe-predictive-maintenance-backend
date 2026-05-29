package com.pfe.predictive.notification.service;

import com.pfe.predictive.core.entity.RiskLevel;
import com.pfe.predictive.notification.entity.Notification;
import com.pfe.predictive.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing notifications triggered by ML predictions.
 * 
 * Notification rules:
 * - LOW risk: No notification
 * - MEDIUM risk: No notification
 * - HIGH risk: Notify MANAGER,ADMIN
 * - CRITICAL risk: Notify TECHNICIAN,MANAGER,ADMIN
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * Create and persist a notification if the risk level warrants it.
     * 
     * @param machineId ID of the machine with the risk prediction
     * @param rulValue predicted RUL in hours
     * @param riskLevel classified risk level (CRITICAL/HIGH/MEDIUM/LOW)
     * @param predictionRecordId ID of the associated prediction record
     */
    @Transactional
    public void notifyIfRequired(Long machineId, Double rulValue, RiskLevel riskLevel, Long predictionRecordId) {
        
        // Only create notifications for HIGH and CRITICAL risk levels
        if (riskLevel == RiskLevel.LOW || riskLevel == RiskLevel.MEDIUM) {
            return;
        }

        String targetRoles;
        String title;
        String body;

        if (riskLevel == RiskLevel.HIGH) {
            targetRoles = "MANAGER,ADMIN";
            title = "⚠️ HIGH RISK: Machine " + machineId + " — schedule maintenance soon";
            body = "Machine " + machineId + ": RUL = " + String.format("%.1f", rulValue) 
                   + " hours | Risk: HIGH | Record ID: " + predictionRecordId;
        } else { // CRITICAL
            targetRoles = "TECHNICIAN,MANAGER,ADMIN";
            title = "🚨 CRITICAL: Machine " + machineId + " requires immediate attention";
            body = "Machine " + machineId + ": RUL = " + String.format("%.1f", rulValue) 
                   + " hours | Risk: CRITICAL | Record ID: " + predictionRecordId;
        }

        // Build and save notification
        Notification notification = Notification.builder()
                .machineId(machineId)
                .title(title)
                .body(body)
                .riskLevel(riskLevel)
                .predictionRecordId(predictionRecordId)
                .targetRoles(targetRoles)
                .isRead(false)
                .build();

        notificationRepository.save(notification);

        log.warn("event=notification_sent machineId={} riskLevel={} targetRoles={} recordId={}",
                machineId, riskLevel, targetRoles, predictionRecordId);
    }
}
