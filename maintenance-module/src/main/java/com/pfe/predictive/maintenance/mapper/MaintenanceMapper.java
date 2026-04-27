package com.pfe.predictive.maintenance.mapper;

import com.pfe.predictive.maintenance.dto.*;
import com.pfe.predictive.core.entity.Maintenance;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Maintenance Module Mapper
 * Converts entities to DTOs and vice versa
 *
 * @author Maintenance Module
 * @version 1.0
 */
@Component
public class MaintenanceMapper {

    /**
     * Convert Maintenance entity to response DTO
     */
    public MaintenanceResponse toResponse(Maintenance maintenance) {
        if (maintenance == null) return null;

        long actualDuration = 0;
        if (maintenance.getStartDate() != null && maintenance.getCompletedDate() != null) {
            actualDuration = ChronoUnit.MINUTES.between(maintenance.getStartDate(), maintenance.getCompletedDate());
        }

        return MaintenanceResponse.builder()
            .id(maintenance.getId())
            .machineId(maintenance.getMachineId())
            .type(maintenance.getType() != null ? maintenance.getType().toString() : null)
            .priority(maintenance.getPriority())
            .description(maintenance.getDescription())
            .status(maintenance.getStatus() != null ? maintenance.getStatus().toString() : null)
            .scheduledDate(maintenance.getScheduledDate())
            .startDate(maintenance.getStartDate())
            .completedDate(maintenance.getCompletedDate())
            .approvedDate(maintenance.getApprovedDate())
            .estimatedDuration(maintenance.getEstimatedDuration())
            .actualDuration(actualDuration > 0 ? (int) actualDuration : null)
            .assignedTechnicianId(maintenance.getAssignedTechnicianId())
            .approvedBy(maintenance.getApprovedBy())
            .notes(maintenance.getNotes())
            .createdDate(maintenance.getCreatedDate())
            .lastModifiedDate(maintenance.getLastModifiedDate())
            .build();
    }

    /**
     * Convert to compact DTO
     */
    public MaintenanceDto toDto(Maintenance maintenance) {
        if (maintenance == null) return null;

        return MaintenanceDto.builder()
            .id(maintenance.getId())
            .machineId(maintenance.getMachineId())
            .type(maintenance.getType() != null ? maintenance.getType().toString() : null)
            .priority(maintenance.getPriority())
            .status(maintenance.getStatus() != null ? maintenance.getStatus().toString() : null)
            .scheduledDate(maintenance.getScheduledDate())
            .description(maintenance.getDescription())
            .build();
    }

    /**
     * Convert list of entities to DTOs
     */
    public List<MaintenanceDto> toDtoList(List<Maintenance> maintenances) {
        if (maintenances == null) return new ArrayList<>();
        return maintenances.stream().map(this::toDto).toList();
    }

    /**
     * Convert page of entities to DTOs
     */
    public Page<MaintenanceDto> toDtoPage(Page<Maintenance> page) {
        return page.map(this::toDto);
    }

    /**
     * Convert page of entities to responses
     */
    public Page<MaintenanceResponse> toResponsePage(Page<Maintenance> page) {
        return page.map(this::toResponse);
    }

    /**
     * Convert list to responses
     */
    public List<MaintenanceResponse> toResponseList(List<Maintenance> maintenances) {
        if (maintenances == null) return new ArrayList<>();
        return maintenances.stream().map(this::toResponse).toList();
    }
}
