package com.pfe.predictive.template.service;

import com.pfe.predictive.audit.service.AuditEventService;
import com.pfe.predictive.common.exception.ResourceNotFoundException;
import com.pfe.predictive.core.entity.Maintenance;
import com.pfe.predictive.core.entity.MaintenanceStatus;
import com.pfe.predictive.core.entity.template.RecurringMaintenanceRule;
import com.pfe.predictive.core.entity.template.WorkOrderTemplate;
import com.pfe.predictive.data.repository.MaintenanceRepository;
import com.pfe.predictive.data.repository.template.RecurringMaintenanceRuleRepository;
import com.pfe.predictive.data.repository.template.WorkOrderTemplateRepository;
import com.pfe.predictive.template.dto.RecurringMaintenanceRuleRequest;
import com.pfe.predictive.template.dto.RecurringMaintenanceRuleResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RecurringMaintenanceService {

    private final RecurringMaintenanceRuleRepository ruleRepository;
    private final WorkOrderTemplateRepository templateRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final AuditEventService auditEventService;

    public RecurringMaintenanceRuleResponse create(RecurringMaintenanceRuleRequest request, String createdBy) {
        WorkOrderTemplate template = templateRepository.findById(request.getWorkOrderTemplateId())
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + request.getWorkOrderTemplateId()));

        RecurringMaintenanceRule rule = RecurringMaintenanceRule.builder()
                .machineId(request.getMachineId())
                .workOrderTemplateId(request.getWorkOrderTemplateId())
                .intervalDays(request.getIntervalDays())
                .assignedTechnicianId(request.getAssignedTechnicianId())
                .nextRunDate(request.getFirstRunDate() != null ? request.getFirstRunDate() : LocalDateTime.now())
                .active(request.getActive() == null || request.getActive())
                .createdBy(createdBy)
                .build();

        RecurringMaintenanceRule saved = ruleRepository.save(rule);
        auditEventService.record(createdBy, "RECURRING_RULE_CREATED", "Machine", saved.getMachineId(),
                "template=" + template.getName() + ", intervalDays=" + saved.getIntervalDays());
        return toResponse(saved, template.getName());
    }

    public void deactivate(Long id) {
        RecurringMaintenanceRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found: " + id));
        rule.setActive(false);
        ruleRepository.save(rule);
    }

    @Transactional(readOnly = true)
    public List<RecurringMaintenanceRuleResponse> getByMachine(Long machineId) {
        return ruleRepository.findByMachineId(machineId).stream()
                .map(rule -> toResponse(rule, templateName(rule.getWorkOrderTemplateId())))
                .toList();
    }

    /** Finds every due, active rule and spawns the next Maintenance work order from its template. */
    public int generateDueMaintenance() {
        List<RecurringMaintenanceRule> due = ruleRepository.findByActiveTrueAndNextRunDateLessThanEqual(LocalDateTime.now());
        int generated = 0;
        for (RecurringMaintenanceRule rule : due) {
            try {
                generateOne(rule);
                generated++;
            } catch (Exception ex) {
                log.error("Failed to generate recurring maintenance for rule {}: {}", rule.getId(), ex.getMessage(), ex);
            }
        }
        return generated;
    }

    private void generateOne(RecurringMaintenanceRule rule) {
        WorkOrderTemplate template = templateRepository.findById(rule.getWorkOrderTemplateId()).orElse(null);
        if (template == null) {
            log.warn("Recurring rule {} references a missing template {} — skipping", rule.getId(), rule.getWorkOrderTemplateId());
            return;
        }

        Maintenance maintenance = new Maintenance();
        maintenance.setMachineId(rule.getMachineId());
        maintenance.setType(template.getType());
        maintenance.setPriority(template.getPriority());
        maintenance.setDescription(template.getDescription() != null && !template.getDescription().isBlank()
                ? template.getDescription() : template.getName());
        maintenance.setStatus(MaintenanceStatus.SCHEDULED);
        maintenance.setScheduledDate(rule.getNextRunDate());
        maintenance.setEstimatedDuration(template.getEstimatedDuration());
        maintenance.setAssignedTechnicianId(rule.getAssignedTechnicianId());
        maintenance.setNotes(template.getDefaultNotes());

        Maintenance saved = maintenanceRepository.save(maintenance);

        rule.setLastGeneratedMaintenanceId(saved.getId());
        rule.setNextRunDate(rule.getNextRunDate().plusDays(rule.getIntervalDays()));
        ruleRepository.save(rule);

        auditEventService.record("SYSTEM", "RECURRING_MAINTENANCE_GENERATED", "Maintenance", saved.getId(),
                "ruleId=" + rule.getId() + ", machineId=" + rule.getMachineId());
        log.info("Generated maintenance #{} from recurring rule {} (machine {}), next run {}",
                saved.getId(), rule.getId(), rule.getMachineId(), rule.getNextRunDate());
    }

    private String templateName(Long templateId) {
        return templateRepository.findById(templateId).map(WorkOrderTemplate::getName).orElse(null);
    }

    private RecurringMaintenanceRuleResponse toResponse(RecurringMaintenanceRule rule, String templateName) {
        return RecurringMaintenanceRuleResponse.builder()
                .id(rule.getId())
                .machineId(rule.getMachineId())
                .workOrderTemplateId(rule.getWorkOrderTemplateId())
                .workOrderTemplateName(templateName)
                .intervalDays(rule.getIntervalDays())
                .assignedTechnicianId(rule.getAssignedTechnicianId())
                .nextRunDate(rule.getNextRunDate())
                .lastGeneratedMaintenanceId(rule.getLastGeneratedMaintenanceId())
                .active(rule.isActive())
                .build();
    }
}
