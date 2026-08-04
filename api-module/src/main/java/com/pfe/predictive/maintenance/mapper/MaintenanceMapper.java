package com.pfe.predictive.maintenance.mapper;

import com.pfe.predictive.core.entity.Maintenance;
import com.pfe.predictive.maintenance.dto.MaintenanceDto;

public final class MaintenanceMapper {

    private MaintenanceMapper() {
    }

    public static MaintenanceDto toDto(Maintenance source) {
        if (source == null) {
            return null;
        }
        MaintenanceDto dto = new MaintenanceDto();
        dto.setId(source.getId());
        dto.setMachineId(source.getMachineId());
        dto.setType(source.getType() != null ? source.getType().name() : null);
        dto.setPriority(source.getPriority() != null ? source.getPriority().name() : null);
        dto.setStatus(source.getStatus() != null ? source.getStatus().name() : null);
        dto.setDescription(source.getDescription());
        dto.setScheduledDate(source.getScheduledDate());
        dto.setStartDate(source.getStartDate());
        dto.setCompletedDate(source.getCompletedDate());
        dto.setEstimatedDuration(source.getEstimatedDuration());
        dto.setApprovedDate(source.getApprovedDate());
        dto.setApprovedBy(source.getApprovedBy());
        dto.setAssignedTechnicianId(source.getAssignedTechnicianId());
        dto.setNotes(source.getNotes());
        dto.setRootCause(source.getRootCause());
        return dto;
    }
}
