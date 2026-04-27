package com.yourpackage.business.controller;

import com.pfe.predictive.maintenancecost.entity.FailureEvent;
import com.pfe.predictive.maintenancecost.entity.Machine;
import com.pfe.predictive.maintenancecost.entity.MaintenanceAction;
import com.pfe.predictive.maintenancecost.entity.MaintenanceBudget;
import com.pfe.predictive.maintenancecost.entity.MaintenancePart;
import com.pfe.predictive.maintenancecost.enums.CriticalityLevel;
import com.pfe.predictive.maintenancecost.enums.MachineStatus;
import com.pfe.predictive.maintenancecost.enums.MaintenanceActionStatus;
import com.pfe.predictive.maintenancecost.enums.MaintenanceActionType;
import com.pfe.predictive.maintenancecost.repository.FailureEventRepository;
import com.pfe.predictive.maintenancecost.repository.MachineRepository;
import com.pfe.predictive.maintenancecost.repository.MaintenanceActionRepository;
import com.pfe.predictive.maintenancecost.repository.MaintenanceBudgetRepository;
import com.pfe.predictive.maintenancecost.repository.MaintenancePartRepository;
import com.yourpackage.business.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/costs")
public class MaintenanceCostCrudController {

    private final MachineRepository machineRepository;
    private final MaintenanceActionRepository maintenanceActionRepository;
    private final MaintenanceBudgetRepository maintenanceBudgetRepository;
    private final MaintenancePartRepository maintenancePartRepository;
    private final FailureEventRepository failureEventRepository;

    public MaintenanceCostCrudController(MachineRepository machineRepository,
                                         MaintenanceActionRepository maintenanceActionRepository,
                                         MaintenanceBudgetRepository maintenanceBudgetRepository,
                                         MaintenancePartRepository maintenancePartRepository,
                                         FailureEventRepository failureEventRepository) {
        this.machineRepository = machineRepository;
        this.maintenanceActionRepository = maintenanceActionRepository;
        this.maintenanceBudgetRepository = maintenanceBudgetRepository;
        this.maintenancePartRepository = maintenancePartRepository;
        this.failureEventRepository = failureEventRepository;
    }

    @GetMapping("/machines")
    public ResponseEntity<List<Machine>> getMachines() {
        return ResponseEntity.ok(machineRepository.findAll());
    }

    @GetMapping("/machines/{id}")
    public ResponseEntity<Machine> getMachine(@PathVariable Long id) {
        return ResponseEntity.ok(findMachine(id));
    }

    @PostMapping("/machines")
    public ResponseEntity<Machine> createMachine(@RequestBody @Valid MachineRequest request) {
        Machine machine = Machine.builder()
                .name(request.getName())
                .location(request.getLocation())
                .status(request.getStatus())
                .hourlyProductionValue(request.getHourlyProductionValue())
                .replacementCost(request.getReplacementCost())
                .criticalityLevel(request.getCriticalityLevel())
                .age(request.getAge())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(machineRepository.save(machine));
    }

    @PutMapping("/machines/{id}")
    public ResponseEntity<Machine> updateMachine(@PathVariable Long id, @RequestBody @Valid MachineRequest request) {
        Machine machine = findMachine(id);
        machine.setName(request.getName());
        machine.setLocation(request.getLocation());
        machine.setStatus(request.getStatus());
        machine.setHourlyProductionValue(request.getHourlyProductionValue());
        machine.setReplacementCost(request.getReplacementCost());
        machine.setCriticalityLevel(request.getCriticalityLevel());
        machine.setAge(request.getAge());
        return ResponseEntity.ok(machineRepository.save(machine));
    }

    @DeleteMapping("/machines/{id}")
    public ResponseEntity<Void> deleteMachine(@PathVariable Long id) {
        machineRepository.delete(findMachine(id));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/actions")
    public ResponseEntity<List<MaintenanceActionResponse>> getActions() {
        return ResponseEntity.ok(maintenanceActionRepository.findAll().stream().map(this::toActionResponse).toList());
    }

    @GetMapping("/actions/{id}")
    public ResponseEntity<MaintenanceActionResponse> getAction(@PathVariable Long id) {
        return ResponseEntity.ok(toActionResponse(findAction(id)));
    }

    @PostMapping("/actions")
    public ResponseEntity<MaintenanceActionResponse> createAction(@RequestBody @Valid MaintenanceActionRequest request) {
        Machine machine = findMachine(request.getMachineId());
        Set<MaintenancePart> parts = resolveParts(request.getPartIds());

        MaintenanceAction action = MaintenanceAction.builder()
                .machine(machine)
                .type(request.getType())
                .estimatedDurationHours(request.getEstimatedDurationHours())
                .laborCostPerHour(request.getLaborCostPerHour())
                .parts(parts)
                .status(request.getStatus())
                .scheduledDate(request.getScheduledDate())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(toActionResponse(maintenanceActionRepository.save(action)));
    }

    @PutMapping("/actions/{id}")
    public ResponseEntity<MaintenanceActionResponse> updateAction(@PathVariable Long id,
                                                                  @RequestBody @Valid MaintenanceActionRequest request) {
        MaintenanceAction action = findAction(id);
        action.setMachine(findMachine(request.getMachineId()));
        action.setType(request.getType());
        action.setEstimatedDurationHours(request.getEstimatedDurationHours());
        action.setLaborCostPerHour(request.getLaborCostPerHour());
        action.setParts(resolveParts(request.getPartIds()));
        action.setStatus(request.getStatus());
        action.setScheduledDate(request.getScheduledDate());

        return ResponseEntity.ok(toActionResponse(maintenanceActionRepository.save(action)));
    }

    @DeleteMapping("/actions/{id}")
    public ResponseEntity<Void> deleteAction(@PathVariable Long id) {
        maintenanceActionRepository.delete(findAction(id));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/failure-events")
    public ResponseEntity<List<FailureEventResponse>> getFailureEvents() {
        return ResponseEntity.ok(failureEventRepository.findAll().stream().map(this::toFailureEventResponse).toList());
    }

    @GetMapping("/failure-events/{id}")
    public ResponseEntity<FailureEventResponse> getFailureEvent(@PathVariable Long id) {
        return ResponseEntity.ok(toFailureEventResponse(findFailureEvent(id)));
    }

    @PostMapping("/failure-events")
    public ResponseEntity<FailureEventResponse> createFailureEvent(@RequestBody @Valid FailureEventRequest request) {
        FailureEvent event = FailureEvent.builder()
                .machine(findMachine(request.getMachineId()))
                .failureType(request.getFailureType())
                .actualDowntimeHours(request.getActualDowntimeHours())
                .occurredAt(request.getOccurredAt())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(toFailureEventResponse(failureEventRepository.save(event)));
    }

    @PutMapping("/failure-events/{id}")
    public ResponseEntity<FailureEventResponse> updateFailureEvent(@PathVariable Long id,
                                                                   @RequestBody @Valid FailureEventRequest request) {
        FailureEvent event = findFailureEvent(id);
        event.setMachine(findMachine(request.getMachineId()));
        event.setFailureType(request.getFailureType());
        event.setActualDowntimeHours(request.getActualDowntimeHours());
        event.setOccurredAt(request.getOccurredAt());

        return ResponseEntity.ok(toFailureEventResponse(failureEventRepository.save(event)));
    }

    @DeleteMapping("/failure-events/{id}")
    public ResponseEntity<Void> deleteFailureEvent(@PathVariable Long id) {
        failureEventRepository.delete(findFailureEvent(id));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/budgets")
    public ResponseEntity<List<MaintenanceBudget>> getBudgets() {
        return ResponseEntity.ok(maintenanceBudgetRepository.findAll());
    }

    @GetMapping("/budgets/{id}")
    public ResponseEntity<MaintenanceBudget> getBudget(@PathVariable Long id) {
        return ResponseEntity.ok(findBudget(id));
    }

    @PostMapping("/budgets")
    public ResponseEntity<MaintenanceBudget> createBudget(@RequestBody @Valid MaintenanceBudgetRequest request) {
        MaintenanceBudget budget = MaintenanceBudget.builder()
                .department(request.getDepartment())
                .period(request.getPeriod())
                .allocatedAmount(request.getAllocatedAmount())
                .spentAmount(request.getSpentAmount())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(maintenanceBudgetRepository.save(budget));
    }

    @PutMapping("/budgets/{id}")
    public ResponseEntity<MaintenanceBudget> updateBudget(@PathVariable Long id,
                                                          @RequestBody @Valid MaintenanceBudgetRequest request) {
        MaintenanceBudget budget = findBudget(id);
        budget.setDepartment(request.getDepartment());
        budget.setPeriod(request.getPeriod());
        budget.setAllocatedAmount(request.getAllocatedAmount());
        budget.setSpentAmount(request.getSpentAmount());
        return ResponseEntity.ok(maintenanceBudgetRepository.save(budget));
    }

    @DeleteMapping("/budgets/{id}")
    public ResponseEntity<Void> deleteBudget(@PathVariable Long id) {
        maintenanceBudgetRepository.delete(findBudget(id));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/parts")
    public ResponseEntity<List<MaintenancePart>> getParts() {
        return ResponseEntity.ok(maintenancePartRepository.findAll());
    }

    @GetMapping("/parts/{id}")
    public ResponseEntity<MaintenancePart> getPart(@PathVariable Long id) {
        return ResponseEntity.ok(findPart(id));
    }

    @PostMapping("/parts")
    public ResponseEntity<MaintenancePart> createPart(@RequestBody @Valid MaintenancePart request) {
        request.setId(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(maintenancePartRepository.save(request));
    }

    @PutMapping("/parts/{id}")
    public ResponseEntity<MaintenancePart> updatePart(@PathVariable Long id, @RequestBody @Valid MaintenancePart request) {
        MaintenancePart existing = findPart(id);
        existing.setName(request.getName());
        existing.setReferenceCode(request.getReferenceCode());
        existing.setUnitCost(request.getUnitCost());
        existing.setStockQuantity(request.getStockQuantity());
        existing.setLeadTimeDays(request.getLeadTimeDays());
        return ResponseEntity.ok(maintenancePartRepository.save(existing));
    }

    @DeleteMapping("/parts/{id}")
    public ResponseEntity<Void> deletePart(@PathVariable Long id) {
        maintenancePartRepository.delete(findPart(id));
        return ResponseEntity.noContent().build();
    }

    private Machine findMachine(Long id) {
        return machineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Machine not found with id " + id));
    }

    private MaintenanceAction findAction(Long id) {
        return maintenanceActionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance action not found with id " + id));
    }

    private FailureEvent findFailureEvent(Long id) {
        return failureEventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Failure event not found with id " + id));
    }

    private MaintenanceBudget findBudget(Long id) {
        return maintenanceBudgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id " + id));
    }

    private MaintenancePart findPart(Long id) {
        return maintenancePartRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Part not found with id " + id));
    }

    private Set<MaintenancePart> resolveParts(List<Long> partIds) {
        if (partIds == null || partIds.isEmpty()) {
            return Set.of();
        }
        List<MaintenancePart> parts = maintenancePartRepository.findAllById(partIds);
        if (parts.size() != partIds.size()) {
            throw new IllegalArgumentException("One or more partIds are invalid");
        }
        return new LinkedHashSet<>(parts);
    }

    private MaintenanceActionResponse toActionResponse(MaintenanceAction action) {
        return new MaintenanceActionResponse(
                action.getId(),
                action.getMachine() != null ? action.getMachine().getId() : null,
                action.getType(),
                action.getEstimatedDurationHours(),
                action.getLaborCostPerHour(),
                action.getParts() == null ? List.of() : action.getParts().stream().map(MaintenancePart::getId).toList(),
                action.getStatus(),
                action.getScheduledDate()
        );
    }

    private FailureEventResponse toFailureEventResponse(FailureEvent event) {
        return new FailureEventResponse(
                event.getId(),
                event.getMachine() != null ? event.getMachine().getId() : null,
                event.getFailureType(),
                event.getActualDowntimeHours(),
                event.getTotalCostIncurred(),
                event.getOccurredAt()
        );
    }

    public static class MachineRequest {
        @NotBlank
        private String name;
        @NotBlank
        private String location;
        @NotNull
        private MachineStatus status;
        @NotNull
        @Positive
        private BigDecimal hourlyProductionValue;
        @NotNull
        @Positive
        private BigDecimal replacementCost;
        @NotNull
        private CriticalityLevel criticalityLevel;
        @NotNull
        @Positive
        private Integer age;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public MachineStatus getStatus() { return status; }
        public void setStatus(MachineStatus status) { this.status = status; }
        public BigDecimal getHourlyProductionValue() { return hourlyProductionValue; }
        public void setHourlyProductionValue(BigDecimal hourlyProductionValue) { this.hourlyProductionValue = hourlyProductionValue; }
        public BigDecimal getReplacementCost() { return replacementCost; }
        public void setReplacementCost(BigDecimal replacementCost) { this.replacementCost = replacementCost; }
        public CriticalityLevel getCriticalityLevel() { return criticalityLevel; }
        public void setCriticalityLevel(CriticalityLevel criticalityLevel) { this.criticalityLevel = criticalityLevel; }
        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }
    }

    public static class MaintenanceActionRequest {
        @NotNull
        private Long machineId;
        @NotNull
        private MaintenanceActionType type;
        @NotNull
        @Positive
        private Double estimatedDurationHours;
        @NotNull
        @Positive
        private BigDecimal laborCostPerHour;
        @NotNull
        private List<Long> partIds;
        @NotNull
        private MaintenanceActionStatus status;
        @NotNull
        private LocalDateTime scheduledDate;

        public Long getMachineId() { return machineId; }
        public void setMachineId(Long machineId) { this.machineId = machineId; }
        public MaintenanceActionType getType() { return type; }
        public void setType(MaintenanceActionType type) { this.type = type; }
        public Double getEstimatedDurationHours() { return estimatedDurationHours; }
        public void setEstimatedDurationHours(Double estimatedDurationHours) { this.estimatedDurationHours = estimatedDurationHours; }
        public BigDecimal getLaborCostPerHour() { return laborCostPerHour; }
        public void setLaborCostPerHour(BigDecimal laborCostPerHour) { this.laborCostPerHour = laborCostPerHour; }
        public List<Long> getPartIds() { return partIds; }
        public void setPartIds(List<Long> partIds) { this.partIds = partIds; }
        public MaintenanceActionStatus getStatus() { return status; }
        public void setStatus(MaintenanceActionStatus status) { this.status = status; }
        public LocalDateTime getScheduledDate() { return scheduledDate; }
        public void setScheduledDate(LocalDateTime scheduledDate) { this.scheduledDate = scheduledDate; }
    }

    public static class FailureEventRequest {
        @NotNull
        private Long machineId;
        @NotBlank
        private String failureType;
        @NotNull
        @Positive
        private Double actualDowntimeHours;
        @NotNull
        private LocalDateTime occurredAt;

        public Long getMachineId() { return machineId; }
        public void setMachineId(Long machineId) { this.machineId = machineId; }
        public String getFailureType() { return failureType; }
        public void setFailureType(String failureType) { this.failureType = failureType; }
        public Double getActualDowntimeHours() { return actualDowntimeHours; }
        public void setActualDowntimeHours(Double actualDowntimeHours) { this.actualDowntimeHours = actualDowntimeHours; }
        public LocalDateTime getOccurredAt() { return occurredAt; }
        public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
    }

    public static class MaintenanceBudgetRequest {
        @NotBlank
        private String department;
        @NotBlank
        private String period;
        @NotNull
        @Positive
        private BigDecimal allocatedAmount;
        @NotNull
        private BigDecimal spentAmount;

        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }
        public String getPeriod() { return period; }
        public void setPeriod(String period) { this.period = period; }
        public BigDecimal getAllocatedAmount() { return allocatedAmount; }
        public void setAllocatedAmount(BigDecimal allocatedAmount) { this.allocatedAmount = allocatedAmount; }
        public BigDecimal getSpentAmount() { return spentAmount; }
        public void setSpentAmount(BigDecimal spentAmount) { this.spentAmount = spentAmount; }
    }

    public static class MaintenanceActionResponse {
        private final Long id;
        private final Long machineId;
        private final MaintenanceActionType type;
        private final Double estimatedDurationHours;
        private final BigDecimal laborCostPerHour;
        private final List<Long> partIds;
        private final MaintenanceActionStatus status;
        private final LocalDateTime scheduledDate;

        public MaintenanceActionResponse(Long id, Long machineId, MaintenanceActionType type,
                                         Double estimatedDurationHours, BigDecimal laborCostPerHour,
                                         List<Long> partIds, MaintenanceActionStatus status, LocalDateTime scheduledDate) {
            this.id = id;
            this.machineId = machineId;
            this.type = type;
            this.estimatedDurationHours = estimatedDurationHours;
            this.laborCostPerHour = laborCostPerHour;
            this.partIds = partIds;
            this.status = status;
            this.scheduledDate = scheduledDate;
        }

        public Long getId() { return id; }
        public Long getMachineId() { return machineId; }
        public MaintenanceActionType getType() { return type; }
        public Double getEstimatedDurationHours() { return estimatedDurationHours; }
        public BigDecimal getLaborCostPerHour() { return laborCostPerHour; }
        public List<Long> getPartIds() { return partIds; }
        public MaintenanceActionStatus getStatus() { return status; }
        public LocalDateTime getScheduledDate() { return scheduledDate; }
    }

    public static class FailureEventResponse {
        private final Long id;
        private final Long machineId;
        private final String failureType;
        private final Double actualDowntimeHours;
        private final BigDecimal totalCostIncurred;
        private final LocalDateTime occurredAt;

        public FailureEventResponse(Long id, Long machineId, String failureType,
                                    Double actualDowntimeHours, BigDecimal totalCostIncurred, LocalDateTime occurredAt) {
            this.id = id;
            this.machineId = machineId;
            this.failureType = failureType;
            this.actualDowntimeHours = actualDowntimeHours;
            this.totalCostIncurred = totalCostIncurred;
            this.occurredAt = occurredAt;
        }

        public Long getId() { return id; }
        public Long getMachineId() { return machineId; }
        public String getFailureType() { return failureType; }
        public Double getActualDowntimeHours() { return actualDowntimeHours; }
        public BigDecimal getTotalCostIncurred() { return totalCostIncurred; }
        public LocalDateTime getOccurredAt() { return occurredAt; }
    }
}