package com.pfe.predictive.task;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Task Request DTO
 * Used for creating and updating tasks
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private String status; // PENDING, IN_PROGRESS, COMPLETED, CANCELLED

    private String priority; // LOW, MEDIUM, HIGH, URGENT

    private Long machineId;

    private Long maintenanceId;

    /**
     * IMPORTANT:
     * Frontend sends technician ID here
     */
    @JsonAlias({"technicianId", "assignedTo", "technicienId"})
    private Long assignedTechnicianId;

    @NotNull(message = "Due date is required")
    private LocalDateTime dueDate;
}