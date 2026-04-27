package com.pfe.predictive.maintenance.controller;

import com.pfe.predictive.core.entity.Maintenance;
import com.pfe.predictive.core.entity.MaintenancePriority;
import com.pfe.predictive.core.entity.MaintenanceStatus;
import com.pfe.predictive.core.entity.MaintenanceType;
import com.pfe.predictive.data.repository.MachineRepository;
import com.pfe.predictive.data.repository.MaintenanceRepository;
import com.pfe.predictive.maintenance.dto.MaintenanceDto;
import com.pfe.predictive.maintenance.dto.MaintenancePageResponse;
import com.pfe.predictive.maintenance.mapper.MaintenanceMapper;
import com.yourpackage.business.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/maintenance")
public class MaintenanceController {

    private final MaintenanceRepository maintenanceRepository;
    private final MachineRepository machineRepository;

    public MaintenanceController(MaintenanceRepository maintenanceRepository,
                                 MachineRepository machineRepository) {
        this.maintenanceRepository = maintenanceRepository;
        this.machineRepository = machineRepository;
    }

    @PostMapping
    public ResponseEntity<MaintenanceDto> createMaintenance(@RequestBody @Valid MaintenanceRequest request) {
        if (!machineRepository.existsById(request.getMachineId())) {
            throw new ResourceNotFoundException("Machine not found with id " + request.getMachineId());
        }

        Maintenance maintenance = new Maintenance();
        maintenance.setMachineId(request.getMachineId());
        maintenance.setType(request.getType());
        maintenance.setPriority(request.getPriority() != null ? request.getPriority() : MaintenancePriority.MEDIUM);
        maintenance.setDescription(request.getDescription());
        maintenance.setStatus(request.getStatus() != null ? request.getStatus() : MaintenanceStatus.SCHEDULED);
        maintenance.setScheduledDate(request.getScheduledDate());
        maintenance.setStartDate(request.getStartDate());
        maintenance.setCompletedDate(request.getCompletedDate());
        maintenance.setApprovedDate(request.getApprovedDate());
        maintenance.setEstimatedDuration(request.getEstimatedDuration());
        maintenance.setAssignedTechnicianId(request.getAssignedTechnicianId());
        maintenance.setApprovedBy(request.getApprovedBy());
        maintenance.setNotes(request.getNotes());

        Maintenance saved = maintenanceRepository.save(maintenance);
        return ResponseEntity.status(HttpStatus.CREATED).body(MaintenanceMapper.toDto(saved));
    }

    @GetMapping
    public ResponseEntity<MaintenancePageResponse> getMaintenance(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "priority", required = false) String priority,
            @RequestParam(value = "machineId", required = false) Long machineId) {

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        Page<Maintenance> result;
        if (machineId != null) {
            result = maintenanceRepository.findByMachineId(machineId, pageable);
        } else {
            MaintenanceStatus parsedStatus = parseStatus(status);
            MaintenancePriority parsedPriority = parsePriority(priority);
            result = maintenanceRepository.findByStatusAndPriority(parsedStatus, parsedPriority, pageable);
        }

        MaintenancePageResponse response = new MaintenancePageResponse(
                result.getContent().stream().map(MaintenanceMapper::toDto).toList(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaintenanceDto> getMaintenanceById(@PathVariable Long id) {
        return maintenanceRepository.findById(id)
                .map(MaintenanceMapper::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private MaintenanceStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return MaintenanceStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private MaintenancePriority parsePriority(String priority) {
        if (priority == null || priority.isBlank()) {
            return null;
        }
        try {
            return MaintenancePriority.valueOf(priority.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static class MaintenanceRequest {
        @NotNull
        private Long machineId;

        @NotNull
        private MaintenanceType type;

        private MaintenancePriority priority;

        @NotBlank
        private String description;

        private MaintenanceStatus status;

        @NotNull
        private java.time.LocalDateTime scheduledDate;

        private java.time.LocalDateTime startDate;
        private java.time.LocalDateTime completedDate;
        private java.time.LocalDateTime approvedDate;

        @Positive
        private Integer estimatedDuration;

        private Long assignedTechnicianId;
        private String approvedBy;
        private String notes;

        public Long getMachineId() {
            return machineId;
        }

        public void setMachineId(Long machineId) {
            this.machineId = machineId;
        }

        public MaintenanceType getType() {
            return type;
        }

        public void setType(MaintenanceType type) {
            this.type = type;
        }

        public MaintenancePriority getPriority() {
            return priority;
        }

        public void setPriority(MaintenancePriority priority) {
            this.priority = priority;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public MaintenanceStatus getStatus() {
            return status;
        }

        public void setStatus(MaintenanceStatus status) {
            this.status = status;
        }

        public java.time.LocalDateTime getScheduledDate() {
            return scheduledDate;
        }

        public void setScheduledDate(java.time.LocalDateTime scheduledDate) {
            this.scheduledDate = scheduledDate;
        }

        public java.time.LocalDateTime getStartDate() {
            return startDate;
        }

        public void setStartDate(java.time.LocalDateTime startDate) {
            this.startDate = startDate;
        }

        public java.time.LocalDateTime getCompletedDate() {
            return completedDate;
        }

        public void setCompletedDate(java.time.LocalDateTime completedDate) {
            this.completedDate = completedDate;
        }

        public java.time.LocalDateTime getApprovedDate() {
            return approvedDate;
        }

        public void setApprovedDate(java.time.LocalDateTime approvedDate) {
            this.approvedDate = approvedDate;
        }

        public Integer getEstimatedDuration() {
            return estimatedDuration;
        }

        public void setEstimatedDuration(Integer estimatedDuration) {
            this.estimatedDuration = estimatedDuration;
        }

        public Long getAssignedTechnicianId() {
            return assignedTechnicianId;
        }

        public void setAssignedTechnicianId(Long assignedTechnicianId) {
            this.assignedTechnicianId = assignedTechnicianId;
        }

        public String getApprovedBy() {
            return approvedBy;
        }

        public void setApprovedBy(String approvedBy) {
            this.approvedBy = approvedBy;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }
}
