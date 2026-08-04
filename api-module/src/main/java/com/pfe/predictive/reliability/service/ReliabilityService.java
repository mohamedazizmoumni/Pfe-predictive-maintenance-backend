package com.pfe.predictive.reliability.service;

import com.pfe.predictive.core.entity.Machine;
import com.pfe.predictive.core.entity.Maintenance;
import com.pfe.predictive.core.entity.MaintenanceType;
import com.pfe.predictive.data.repository.MachineRepository;
import com.pfe.predictive.data.repository.MaintenanceRepository;
import com.pfe.predictive.reliability.dto.FailureRecordDto;
import com.pfe.predictive.reliability.dto.MachineReliabilityDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MTBF/MTTR/root-cause analytics, computed directly from real Maintenance
 * records (type CORRECTIVE/EMERGENCY, i.e. failure-driven repairs — not
 * PREVENTIVE). Deliberately does not depend on the separate, orphaned
 * FailureEvent/maintenancecost.Machine model (that model is unreachable from
 * any controller and uses its own parallel Machine entity — wiring into it
 * would risk the existing, working budget-automation listener on that side).
 *
 * This is Manager-role enrichment (enterprise scope reorientation
 * 2026-08-01) — the "Reliability Engineer" responsibilities from the
 * blueprint, folded into Manager rather than a new role.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReliabilityService {

    private static final List<MaintenanceType> FAILURE_TYPES = List.of(MaintenanceType.CORRECTIVE, MaintenanceType.EMERGENCY);
    private static final int MAX_FAILURE_RECORDS = 200;

    private final MaintenanceRepository maintenanceRepository;
    private final MachineRepository machineRepository;

    public List<MachineReliabilityDto> getFleetReliability() {
        List<Maintenance> failures = maintenanceRepository
                .findByTypeInAndCompletedDateIsNotNullOrderByCompletedDateAsc(FAILURE_TYPES);

        Map<Long, List<Maintenance>> byMachine = new HashMap<>();
        for (Maintenance m : failures) {
            byMachine.computeIfAbsent(m.getMachineId(), id -> new ArrayList<>()).add(m);
        }

        List<MachineReliabilityDto> results = new ArrayList<>();
        for (Map.Entry<Long, List<Maintenance>> entry : byMachine.entrySet()) {
            Long machineId = entry.getKey();
            List<Maintenance> records = entry.getValue(); // already ascending by completedDate

            Machine machine = machineRepository.findById(machineId).orElse(null);

            Double mtbfHours = computeMtbfHours(records);
            Double mttrHours = computeMttrHours(records);
            LocalDateTime lastFailureAt = records.get(records.size() - 1).getCompletedDate();

            results.add(MachineReliabilityDto.builder()
                    .machineId(machineId)
                    .machineName(machine != null ? machine.getName() : ("Machine #" + machineId))
                    .mtbfHours(mtbfHours)
                    .mttrHours(mttrHours)
                    .failureCount(records.size())
                    .lastFailureAt(lastFailureAt)
                    .build());
        }

        // Worst (lowest, most-frequent-failure) MTBF first; machines with too
        // little data (null MTBF) sort last rather than being hidden.
        results.sort(Comparator.comparing(
                MachineReliabilityDto::getMtbfHours,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return results;
    }

    public List<FailureRecordDto> getFailureRecords(Long machineId) {
        List<Maintenance> records = machineId != null
                ? maintenanceRepository.findByMachineIdAndTypeInAndCompletedDateIsNotNullOrderByCompletedDateDesc(machineId, FAILURE_TYPES)
                : maintenanceRepository.findByTypeInAndCompletedDateIsNotNullOrderByCompletedDateDesc(FAILURE_TYPES);

        return records.stream()
                .limit(MAX_FAILURE_RECORDS)
                .map(this::toFailureRecordDto)
                .toList();
    }

    private FailureRecordDto toFailureRecordDto(Maintenance m) {
        Machine machine = machineRepository.findById(m.getMachineId()).orElse(null);
        Double repairHours = (m.getStartDate() != null && m.getCompletedDate() != null)
                ? Duration.between(m.getStartDate(), m.getCompletedDate()).toMinutes() / 60.0
                : null;

        return FailureRecordDto.builder()
                .maintenanceId(m.getId())
                .machineId(m.getMachineId())
                .machineName(machine != null ? machine.getName() : ("Machine #" + m.getMachineId()))
                .type(m.getType() != null ? m.getType().name() : null)
                .description(m.getDescription())
                .rootCause(m.getRootCause())
                .repairHours(repairHours)
                .completedDate(m.getCompletedDate())
                .build();
    }

    private Double computeMttrHours(List<Maintenance> records) {
        List<Double> repairHours = records.stream()
                .filter(m -> m.getStartDate() != null && m.getCompletedDate() != null)
                .map(m -> Duration.between(m.getStartDate(), m.getCompletedDate()).toMinutes() / 60.0)
                .toList();
        if (repairHours.isEmpty()) {
            return null;
        }
        return repairHours.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private Double computeMtbfHours(List<Maintenance> records) {
        if (records.size() < 2) {
            return null;
        }
        double totalGapHours = 0.0;
        for (int i = 1; i < records.size(); i++) {
            LocalDateTime previous = records.get(i - 1).getCompletedDate();
            LocalDateTime current = records.get(i).getCompletedDate();
            totalGapHours += Duration.between(previous, current).toMinutes() / 60.0;
        }
        return totalGapHours / (records.size() - 1);
    }
}
