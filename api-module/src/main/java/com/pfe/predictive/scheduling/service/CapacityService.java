package com.pfe.predictive.scheduling.service;

import com.pfe.predictive.core.entity.Maintenance;
import com.pfe.predictive.core.entity.MaintenanceStatus;
import com.pfe.predictive.core.entity.User;
import com.pfe.predictive.data.repository.MaintenanceRepository;
import com.pfe.predictive.data.repository.UserRepository;
import com.pfe.predictive.scheduling.dto.TechnicianCapacityDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Per-technician workload, so a Manager can see who has room before
 * assigning a new work order — "Maintenance Planner" capacity-planning
 * responsibility folded into Manager (2026-08-01 scope reorientation).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CapacityService {

    private static final List<MaintenanceStatus> OPEN_STATUSES = List.of(MaintenanceStatus.SCHEDULED, MaintenanceStatus.IN_PROGRESS);

    private final UserRepository userRepository;
    private final MaintenanceRepository maintenanceRepository;

    public List<TechnicianCapacityDto> getFleetCapacity() {
        List<User> technicians = userRepository.findUsersByRoleName("TECHNICIAN");

        return technicians.stream()
                .map(this::toCapacityDto)
                .sorted(Comparator.comparingLong(TechnicianCapacityDto::getOpenJobCount).reversed())
                .toList();
    }

    private TechnicianCapacityDto toCapacityDto(User technician) {
        List<Maintenance> openJobs = maintenanceRepository
                .findByAssignedTechnicianIdAndStatusIn(technician.getId(), OPEN_STATUSES);

        int totalHours = openJobs.stream()
                .map(Maintenance::getEstimatedDuration)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        return TechnicianCapacityDto.builder()
                .technicianId(technician.getId())
                .technicianName(technician.getDisplayName() != null ? technician.getDisplayName() : technician.getUsername())
                .username(technician.getUsername())
                .openJobCount(openJobs.size())
                .totalEstimatedHours(totalHours)
                .build();
    }
}
