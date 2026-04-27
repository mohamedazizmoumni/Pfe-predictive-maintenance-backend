package com.pfe.predictive.maintenance.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Maintenance Module Request/Response DTOs
 * Used for API request validation and response serialization
 *
 * @author Maintenance Module
 * @version 1.0
 */

// ==================== REQUEST DTOS ====================

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceRequest {
    @NotNull(message = "Machine ID is required")
    private Long machineId;

    @NotBlank(message = "Type is required")
    private String type; // PREVENTIVE, CORRECTIVE, EMERGENCY

    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 500, message = "Description must be 10-500 characters")
    private String description;

    @NotNull(message = "Scheduled date is required")
    private LocalDateTime scheduledDate;

    @Min(value = 15, message = "Estimated duration must be at least 15 minutes")
    @Max(value = 480, message = "Estimated duration cannot exceed 480 minutes")
    private Integer estimatedDuration; // in minutes

    private Long assignedTechnicianId;

    @NotBlank(message = "Priority is required")
    private String priority; // LOW, MEDIUM, HIGH, CRITICAL

    private String notes;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceStatusChangeRequest {
    @NotBlank(message = "Action is required")
    private String action; // START, COMPLETE, APPROVE, REJECT, CANCEL

    private String notes; // For completion, rejection, cancellation
}

// ==================== RESPONSE DTOS ====================

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceResponse {
    private Long id;

    private Long machineId;

    private String machineSerialNumber;

    private String type;

    private String priority;

    private String description;

    private String status;

    private LocalDateTime scheduledDate;

    private LocalDateTime startDate;

    private LocalDateTime completedDate;

    private LocalDateTime approvedDate;

    private Integer estimatedDuration; // minutes

    private Integer actualDuration; // minutes

    private Long assignedTechnicianId;

    private String assignedTechnicianName;

    private String approvedBy;

    private String notes;

    private LocalDateTime createdDate;

    private LocalDateTime lastModifiedDate;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceDto {
    private Long id;

    private Long machineId;

    private String type;

    private String priority;

    private String status;

    private LocalDateTime scheduledDate;

    private String description;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceStatsResponse {
    private long totalMaintenance;

    private long scheduledCount;

    private long inProgressCount;

    private long completedCount;

    private long approvedCount;

    private long cancelledCount;

    private long overdueCount;

    private double completionPercentage;

    private double approvalPercentage;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceScheduleResponse {
    private Long machineId;

    private String machineSerialNumber;

    private int pendingMaintenanceCount;

    private LocalDateTime nextScheduledDate;

    private String nextMaintenanceType;

    private String nextMaintenancePriority;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceHistoryResponse {
    private Long machineId;

    private int totalTasksCompleted;

    private double averageDurationMinutes;

    private LocalDateTime lastCompletedDate;

    private String lastCompletedType;

    private int failedApprovalCount;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechnicianWorkloadResponse {
    private Long technicianId;

    private String technicianName;

    private int assignedTasksCount;

    private int completedTasksCount;

    private int pendingTasksCount;

    private double completionRate;

    private LocalDateTime lastCompletedDate;
}
