package com.pfe.predictive.machine.service;

import com.pfe.predictive.core.entity.Machine;
import com.pfe.predictive.data.repository.MachineRepository;
import com.pfe.predictive.machine.dto.MachineDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MachineQueryService {

    private final MachineRepository machineRepository;

    public MachineQueryService(MachineRepository machineRepository) {
        this.machineRepository = machineRepository;
    }

    public List<MachineDTO> findAll() {
        return machineRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    public Optional<MachineDTO> findById(Long id) {
        return machineRepository.findById(id).map(this::toDto);
    }

    private MachineDTO toDto(Machine machine) {
        MachineDTO dto = new MachineDTO();
        dto.setId(machine.getId());
        dto.setSerialNumber(machine.getSerialNumber());
        dto.setName(machine.getName());
        dto.setDescription(machine.getDescription());
        dto.setModel(machine.getModel());
        dto.setManufacturer(machine.getManufacturer());
        dto.setLocation(machine.getLocation());
        dto.setCategory(machine.getCategory());
        dto.setSubCategory(machine.getSubCategory());
        dto.setStatus(machine.getStatus());
        if (machine.getPhotoPath() != null && !machine.getPhotoPath().isBlank()) {
            dto.setPhotoUrl("/api/v1/machines/" + machine.getId() + "/photo");
        }
        dto.setInstallationDate(machine.getInstallationDate());
        dto.setLastMaintenanceDate(machine.getLastMaintenanceDate());
        dto.setNextMaintenanceDate(machine.getNextMaintenanceDate());
        dto.setOperatingHours(machine.getOperatingHours());
        dto.setRiskScore(machine.getRiskScore());
        dto.setCreatedDate(machine.getCreatedDate());
        return dto;
    }
}
