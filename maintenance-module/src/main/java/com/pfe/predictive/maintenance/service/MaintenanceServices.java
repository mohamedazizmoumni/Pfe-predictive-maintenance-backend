package com.pfe.predictive.maintenance.service;

import com.pfe.predictive.core.entity.Maintenance;
import com.pfe.predictive.core.entity.MaintenanceStatus;
import com.pfe.predictive.core.entity.MaintenanceType;
import com.pfe.predictive.maintenance.dto.*;
import com.pfe.predictive.maintenance.repository.*;
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
public class MaintenanceService {

    private final MaintenanceRepository maintenanceRepository;
    private final MaintenanceMapper maintenanceMapper;

    public MaintenanceResponse createMaintenance(MaintenanceRequest request) {
        log.info("Creating maintenance for machine: {}", request.getMachineId());
        Maintenance maintenance = maintenanceMapper.toEntity(request);
        Maintenance saved = maintenanceRepository.save(maintenance);
        return maintenanceMapper.toResponse(saved);
    }

    public MaintenanceResponse getMaintenanceById(Long id) {
        Maintenance maintenance = maintenanceRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Maintenance not found"));
        return maintenanceMapper.toResponse(maintenance);
    }

    public List<MaintenanceResponse> getAllMaintenance() {
        return maintenanceRepository.findAll()
            .stream()
            .map(maintenanceMapper::toResponse)
            .toList();
    }

    public MaintenanceResponse updateMaintenance(Long id, MaintenanceUpdateRequest request) {
        log.info("Updating maintenance: {}", id);
        Maintenance maintenance = maintenanceRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Maintenance not found"));
        
        maintenance.setDescription(request.getDescription());
        maintenance.setStatus(MaintenanceStatus.valueOf(request.getStatus()));
        maintenance.setActualHours(request.getActualHours());
        maintenance.setNotes(request.getNotes());

        Maintenance updated = maintenanceRepository.save(maintenance);
        return maintenanceMapper.toResponse(updated);
    }

    public void deleteMaintenance(Long id) {
        maintenanceRepository.deleteById(id);
    }
}

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MaintenanceQueryService {

    private final MaintenanceRepository maintenanceRepository;

    public long countUpcomingMaintenance() {
        return maintenanceRepository.countByStatus(MaintenanceStatus.PLANNED);
    }

    public List<Maintenance> getUpcomingMaintenance(int limit) {
        return maintenanceRepository.findByStatusOrderByScheduledDateAsc(MaintenanceStatus.PLANNED)
            .stream()
            .limit(limit)
            .toList();
    }

    public double calculateAvailability() {
        // Placeholder - would calculate from maintenenace records
        return 95.5;
    }

    public double getComplianceRate() {
        // Placeholder - percentage of scheduled maintenance completed on time
        return 88.0;
    }

    public double calculateMTBF() {
        // Mean Time Between Failures - placeholder
        return 720.0; // hours
    }

    public double calculateMTTR() {
        // Mean Time To Repair - placeholder
        return 4.5; // hours
    }

    public double calculateOEE() {
        // Overall Equipment Effectiveness - placeholder
        return 82.3;
    }
}
