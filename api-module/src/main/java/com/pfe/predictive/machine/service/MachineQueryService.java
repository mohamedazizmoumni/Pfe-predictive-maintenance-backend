package com.pfe.predictive.machine.service;

import com.pfe.predictive.core.entity.Machine;
import com.pfe.predictive.core.entity.User;
import com.pfe.predictive.data.repository.MachineRepository;
import com.pfe.predictive.data.repository.MachineTechnicianAssignmentRepository;
import com.pfe.predictive.data.repository.UserRepository;
import com.pfe.predictive.machine.dto.MachineDTO;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class MachineQueryService {

    private static final Set<String> ELEVATED_AUTHORITIES = Set.of("ROLE_MANAGER", "ROLE_ADMIN", "ROLE_SUPER_ADMIN");
    private static final String TECHNICIAN_AUTHORITY = "ROLE_TECHNICIAN";

    private final MachineRepository machineRepository;
    private final UserRepository userRepository;
    private final MachineTechnicianAssignmentRepository assignmentRepository;

    public MachineQueryService(MachineRepository machineRepository,
                                UserRepository userRepository,
                                MachineTechnicianAssignmentRepository assignmentRepository) {
        this.machineRepository = machineRepository;
        this.userRepository = userRepository;
        this.assignmentRepository = assignmentRepository;
    }

    public List<MachineDTO> findAll() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (isTechnicianOnly(auth)) {
            Long technicianId = resolveUserId(auth);
            List<Long> assignedMachineIds = assignmentRepository.findMachineIdsByTechnicianId(technicianId);
            return machineRepository.findAllById(assignedMachineIds)
                    .stream()
                    .map(this::toDto)
                    .toList();
        }

        return machineRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    public Optional<MachineDTO> findById(Long id) {
        Optional<Machine> machine = machineRepository.findById(id);
        if (machine.isEmpty()) {
            return Optional.empty();
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (isTechnicianOnly(auth)) {
            Long technicianId = resolveUserId(auth);
            if (!assignmentRepository.existsByMachineIdAndTechnicianId(id, technicianId)) {
                throw new AccessDeniedException("You are not assigned to this machine");
            }
        }

        return machine.map(this::toDto);
    }

    private boolean isElevated(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(ELEVATED_AUTHORITIES::contains);
    }

    private boolean isTechnicianOnly(Authentication auth) {
        if (auth == null || isElevated(auth)) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(TECHNICIAN_AUTHORITY::equals);
    }

    private Long resolveUserId(Authentication auth) {
        return userRepository.findByUsername(auth.getName())
                .map(User::getId)
                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found: " + auth.getName()));
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
