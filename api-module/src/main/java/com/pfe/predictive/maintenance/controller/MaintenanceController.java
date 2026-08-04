package com.pfe.predictive.maintenance.controller;

import com.pfe.predictive.audit.service.AuditEventService;
import com.pfe.predictive.common.service.EmailService;
import com.pfe.predictive.core.entity.Maintenance;
import com.pfe.predictive.core.entity.MaintenancePriority;
import com.pfe.predictive.core.entity.MaintenanceStatus;
import com.pfe.predictive.core.entity.MaintenanceType;
import com.pfe.predictive.core.entity.User;
import com.pfe.predictive.data.repository.UserRepository;
import com.pfe.predictive.data.repository.MachineRepository;
import com.pfe.predictive.data.repository.MaintenanceRepository;
import com.pfe.predictive.maintenance.dto.MaintenanceDto;
import com.pfe.predictive.maintenance.dto.MaintenancePageResponse;
import com.pfe.predictive.maintenance.mapper.MaintenanceMapper;
import com.pfe.predictive.common.exception.ResourceNotFoundException;
import com.pfe.predictive.security.PermissionConstants;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@Slf4j   
@RestController
@RequestMapping("/api/v1/maintenance")
public class MaintenanceController {

    private final MaintenanceRepository maintenanceRepository;
    private final MachineRepository machineRepository;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final AuditEventService auditEventService;

    public MaintenanceController(MaintenanceRepository maintenanceRepository,
                                 MachineRepository machineRepository,
                                 EmailService emailService,
                                 UserRepository userRepository,
                                 AuditEventService auditEventService) {
        this.maintenanceRepository = maintenanceRepository;
        this.machineRepository = machineRepository;
        this.emailService = emailService;
        this.userRepository = userRepository;
        this.auditEventService = auditEventService;
    }

    @PostMapping
    @PreAuthorize(PermissionConstants.PERM_MAINTENANCE_CREATE)
    public ResponseEntity<MaintenanceDto> createMaintenance(@RequestBody @Valid MaintenanceRequest request) {
        if (!machineRepository.existsById(request.getMachineId())) {
            throw new ResourceNotFoundException("Machine not found with id " + request.getMachineId());
        }

        Maintenance maintenance = new Maintenance();
        maintenance.setMachineId(request.getMachineId());
        maintenance.setType(request.getType() != null ? request.getType() : MaintenanceType.CORRECTIVE);
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

        if (request.getAssignedTechnicianId() != null) {
            sendAssignmentEmail(saved);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(MaintenanceMapper.toDto(saved));
    }

    @GetMapping
    @PreAuthorize(PermissionConstants.PERM_MAINTENANCE_READ)
    public ResponseEntity<MaintenancePageResponse> getMaintenance(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "priority", required = false) String priority,
            @RequestParam(value = "machineId", required = false) Long machineId,
            @RequestParam(value = "assignedTechnicianId", required = false) Long assignedTechnicianId) {

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        Page<Maintenance> result;
        if (machineId != null) {
            result = maintenanceRepository.findByMachineId(machineId, pageable);
        } else {
            MaintenanceStatus parsedStatus = parseStatus(status);
            MaintenancePriority parsedPriority = parsePriority(priority);
            result = maintenanceRepository.findByStatusAndPriority(parsedStatus, parsedPriority, assignedTechnicianId, pageable);
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
    @PreAuthorize(PermissionConstants.PERM_MAINTENANCE_READ)
    public ResponseEntity<MaintenanceDto> getMaintenanceById(@PathVariable Long id) {
        return maintenanceRepository.findById(id)
                .map(MaintenanceMapper::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/start")
    @PreAuthorize(PermissionConstants.PERM_MAINTENANCE_UPDATE)
    public ResponseEntity<MaintenanceDto> startMaintenance(@PathVariable Long id) {
        Maintenance maintenance = maintenanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance not found with id " + id));
        maintenance.setStatus(MaintenanceStatus.IN_PROGRESS);
        maintenance.setStartDate(java.time.LocalDateTime.now());
        return ResponseEntity.ok(MaintenanceMapper.toDto(maintenanceRepository.save(maintenance)));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize(PermissionConstants.PERM_MAINTENANCE_UPDATE)
    public ResponseEntity<MaintenanceDto> completeMaintenance(@PathVariable Long id) {
        Maintenance maintenance = maintenanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance not found with id " + id));
        maintenance.setStatus(MaintenanceStatus.COMPLETED);
        maintenance.setCompletedDate(java.time.LocalDateTime.now());
        return ResponseEntity.ok(MaintenanceMapper.toDto(maintenanceRepository.save(maintenance)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize(PermissionConstants.PERM_MAINTENANCE_APPROVE)
    public ResponseEntity<MaintenanceDto> approveMaintenance(
            @PathVariable Long id,
            @RequestParam String approvedBy,
            Authentication authentication) {
        Maintenance maintenance = maintenanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance not found with id " + id));

        boolean isPrivileged = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER")
                        || a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_SUPER_ADMIN"));

        if (!isPrivileged) {
            // Technician caller: only the assigned technician may accept their own task.
            User caller = userRepository.findByUsername(authentication.getName())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authentication.getName()));
            if (maintenance.getAssignedTechnicianId() == null
                    || !maintenance.getAssignedTechnicianId().equals(caller.getId())) {
                throw new AccessDeniedException("You can only accept a task assigned to you");
            }
        }

        maintenance.setStatus(MaintenanceStatus.APPROVED);
        maintenance.setApprovedDate(java.time.LocalDateTime.now());
        maintenance.setApprovedBy(approvedBy);
        Maintenance approved = maintenanceRepository.save(maintenance);
        auditEventService.record(approvedBy, "MAINTENANCE_APPROVED", "Maintenance", approved.getId(), null);
        return ResponseEntity.ok(MaintenanceMapper.toDto(approved));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize(PermissionConstants.PERM_MAINTENANCE_UPDATE)
    public ResponseEntity<MaintenanceDto> cancelMaintenance(@PathVariable Long id) {
        Maintenance maintenance = maintenanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance not found with id " + id));
        maintenance.setStatus(MaintenanceStatus.CANCELLED);
        Maintenance cancelled = maintenanceRepository.save(maintenance);
        auditEventService.record("MAINTENANCE_CANCELLED", "Maintenance", cancelled.getId(), null);
        return ResponseEntity.ok(MaintenanceMapper.toDto(cancelled));
    }

    @PutMapping("/{id}")
    @PreAuthorize(PermissionConstants.PERM_MAINTENANCE_UPDATE)
    public ResponseEntity<MaintenanceDto> updateMaintenance(
            @PathVariable Long id,
            @RequestBody MaintenanceRequest request) {

        Maintenance maintenance = maintenanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance not found with id " + id));

        Long previousAssignedTechnicianId = maintenance.getAssignedTechnicianId();
        MaintenanceStatus previousStatus = maintenance.getStatus();

        if (request.getType() != null) maintenance.setType(request.getType());
        if (request.getPriority() != null) maintenance.setPriority(request.getPriority());
        if (request.getDescription() != null && !request.getDescription().isBlank())
            maintenance.setDescription(request.getDescription());
        if (request.getStatus() != null) maintenance.setStatus(request.getStatus());
        if (request.getScheduledDate() != null) maintenance.setScheduledDate(request.getScheduledDate());
        if (request.getStartDate() != null) maintenance.setStartDate(request.getStartDate());
        if (request.getCompletedDate() != null) maintenance.setCompletedDate(request.getCompletedDate());
        if (request.getApprovedDate() != null) maintenance.setApprovedDate(request.getApprovedDate());
        if (request.getEstimatedDuration() != null) maintenance.setEstimatedDuration(request.getEstimatedDuration());
        if (request.getAssignedTechnicianId() != null)
            maintenance.setAssignedTechnicianId(request.getAssignedTechnicianId());
        if (request.getApprovedBy() != null) maintenance.setApprovedBy(request.getApprovedBy());
        if (request.getNotes() != null) maintenance.setNotes(request.getNotes());
        if (request.getRootCause() != null) maintenance.setRootCause(request.getRootCause());

        Maintenance saved = maintenanceRepository.save(maintenance);

        if (request.getAssignedTechnicianId() != null
                && !Objects.equals(previousAssignedTechnicianId, saved.getAssignedTechnicianId())) {
            sendAssignmentEmail(saved);
            auditEventService.record("MAINTENANCE_TECHNICIAN_ASSIGNED", "Maintenance", saved.getId(),
                    "technicianId=" + saved.getAssignedTechnicianId());
        }
        if (previousStatus != saved.getStatus()) {
            auditEventService.record("MAINTENANCE_STATUS_CHANGED", "Maintenance", saved.getId(),
                    previousStatus + " -> " + saved.getStatus());
        }

        return ResponseEntity.ok(MaintenanceMapper.toDto(saved));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(PermissionConstants.PERM_MAINTENANCE_DELETE)
    public ResponseEntity<Void> deleteMaintenance(@PathVariable Long id) {
        Maintenance maintenance = maintenanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance not found with id " + id));
        maintenanceRepository.delete(maintenance);
        auditEventService.record("MAINTENANCE_DELETED", "Maintenance", id, null);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private void sendAssignmentEmail(Maintenance maintenance) {
        try {
            Long technicianId = maintenance.getAssignedTechnicianId();
            if (technicianId == null) return;

            User technician = userRepository.findById(technicianId).orElse(null);
            if (technician == null || technician.getEmail() == null || technician.getEmail().isBlank()) {
                log.warn("Skipping maintenance email #{} — technician email is missing", maintenance.getId());
                return;
            }

            String recipientName = technician.getFirstName();
            if (recipientName == null || recipientName.isBlank()) recipientName = technician.getUsername();
            if (recipientName == null || recipientName.isBlank()) recipientName = "Technician";

            String subject = "New Maintenance Task Assigned — #" + maintenance.getId();

            String body = """
                    Hello %s,

                    A new maintenance task has been assigned to you.

                    ══════════════════════════════════════════
                    TASK DETAILS
                    ══════════════════════════════════════════

                    Task ID        : %d
                    Machine ID     : %d
                    Type           : %s
                    Priority       : %s
                    Status         : %s
                    Scheduled Date : %s

                    Please log in to the Predictive Maintenance Platform
                    to review and manage this task.

                    Best regards,
                    Predictive Maintenance System
                    """.formatted(
                    recipientName,
                    maintenance.getId(),
                    maintenance.getMachineId(),
                    maintenance.getType(),
                    maintenance.getPriority(),
                    maintenance.getStatus(),
                    maintenance.getScheduledDate()
            );

            emailService.sendSimpleEmail(technician.getEmail(), subject, body);

        } catch (Exception ex) {
            log.error("Failed to send maintenance assignment email for task #{}", maintenance.getId(), ex);
        }
    }

    private MaintenanceStatus parseStatus(String status) {
        if (status == null || status.isBlank()) return null;
        try { return MaintenanceStatus.valueOf(status.toUpperCase()); }
        catch (IllegalArgumentException ex) { return null; }
    }

    private MaintenancePriority parsePriority(String priority) {
        if (priority == null || priority.isBlank()) return null;
        try { return MaintenancePriority.valueOf(priority.toUpperCase()); }
        catch (IllegalArgumentException ex) { return null; }
    }

    // =========================================================================
    // INNER REQUEST CLASS
    // =========================================================================

    public static class MaintenanceRequest {
        @NotNull  private Long machineId;
                  private MaintenanceType type;
                  private MaintenancePriority priority;
        @NotBlank private String description;
                  private MaintenanceStatus status;
        @NotNull  private java.time.LocalDateTime scheduledDate;
                  private java.time.LocalDateTime startDate;
                  private java.time.LocalDateTime completedDate;
                  private java.time.LocalDateTime approvedDate;
        @Positive private Integer estimatedDuration;
                  private Long assignedTechnicianId;
                  private String approvedBy;
                  private String notes;
                  private String rootCause;

        public Long getMachineId() { return machineId; }
        public void setMachineId(Long machineId) { this.machineId = machineId; }
        public MaintenanceType getType() { return type; }
        public void setType(MaintenanceType type) { this.type = type; }
        public MaintenancePriority getPriority() { return priority; }
        public void setPriority(MaintenancePriority priority) { this.priority = priority; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public MaintenanceStatus getStatus() { return status; }
        public void setStatus(MaintenanceStatus status) { this.status = status; }
        public java.time.LocalDateTime getScheduledDate() { return scheduledDate; }
        public void setScheduledDate(java.time.LocalDateTime scheduledDate) { this.scheduledDate = scheduledDate; }
        public java.time.LocalDateTime getStartDate() { return startDate; }
        public void setStartDate(java.time.LocalDateTime startDate) { this.startDate = startDate; }
        public java.time.LocalDateTime getCompletedDate() { return completedDate; }
        public void setCompletedDate(java.time.LocalDateTime completedDate) { this.completedDate = completedDate; }
        public java.time.LocalDateTime getApprovedDate() { return approvedDate; }
        public void setApprovedDate(java.time.LocalDateTime approvedDate) { this.approvedDate = approvedDate; }
        public Integer getEstimatedDuration() { return estimatedDuration; }
        public void setEstimatedDuration(Integer estimatedDuration) { this.estimatedDuration = estimatedDuration; }
        public Long getAssignedTechnicianId() { return assignedTechnicianId; }
        public void setAssignedTechnicianId(Long assignedTechnicianId) { this.assignedTechnicianId = assignedTechnicianId; }
        public String getApprovedBy() { return approvedBy; }
        public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
        public String getRootCause() { return rootCause; }
        public void setRootCause(String rootCause) { this.rootCause = rootCause; }
    }
}