package com.pfe.predictive.machine.service;

import com.pfe.predictive.core.entity.Machine;
import com.pfe.predictive.core.entity.MachineTechnicianAssignment;
import com.pfe.predictive.core.entity.Role;
import com.pfe.predictive.core.entity.User;
import com.pfe.predictive.data.repository.MachineRepository;
import com.pfe.predictive.data.repository.MachineTechnicianAssignmentRepository;
import com.pfe.predictive.data.repository.UserRepository;
import com.pfe.predictive.machine.dto.MachineTechnicianDTO;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MachineAssignmentService {

    private final MachineRepository machineRepository;
    private final UserRepository userRepository;
    private final MachineTechnicianAssignmentRepository assignmentRepository;

    public MachineAssignmentService(MachineRepository machineRepository,
                                     UserRepository userRepository,
                                     MachineTechnicianAssignmentRepository assignmentRepository) {
        this.machineRepository = machineRepository;
        this.userRepository = userRepository;
        this.assignmentRepository = assignmentRepository;
    }

    public MachineTechnicianDTO assign(Long machineId, Long technicianId, String assignedByUsername) {
        Machine machine = machineRepository.findById(machineId)
                .orElseThrow(() -> new EntityNotFoundException("Machine not found: " + machineId));
        User technician = userRepository.findById(technicianId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + technicianId));

        boolean hasTechnicianRole = technician.getRoles().stream()
                .map(Role::getName)
                .anyMatch(name -> name != null && name.equalsIgnoreCase("TECHNICIAN"));
        if (!hasTechnicianRole) {
            throw new IllegalArgumentException("User " + technicianId + " does not have the TECHNICIAN role");
        }

        if (assignmentRepository.existsByMachineIdAndTechnicianId(machineId, technicianId)) {
            throw new IllegalArgumentException("Technician " + technicianId + " is already assigned to machine " + machineId);
        }

        User assignedBy = userRepository.findByUsername(assignedByUsername)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + assignedByUsername));

        MachineTechnicianAssignment assignment = new MachineTechnicianAssignment(machine, technician, assignedBy);
        return toDto(assignmentRepository.save(assignment));
    }

    public void unassign(Long machineId, Long technicianId) {
        if (!assignmentRepository.existsByMachineIdAndTechnicianId(machineId, technicianId)) {
            throw new IllegalArgumentException("Technician " + technicianId + " is not assigned to machine " + machineId);
        }
        assignmentRepository.deleteByMachineIdAndTechnicianId(machineId, technicianId);
    }

    public List<MachineTechnicianDTO> listForMachine(Long machineId) {
        if (!machineRepository.existsById(machineId)) {
            throw new EntityNotFoundException("Machine not found: " + machineId);
        }
        return assignmentRepository.findByMachineId(machineId).stream()
                .map(this::toDto)
                .toList();
    }

    private MachineTechnicianDTO toDto(MachineTechnicianAssignment assignment) {
        MachineTechnicianDTO dto = new MachineTechnicianDTO();
        dto.setId(assignment.getId());
        dto.setMachineId(assignment.getMachine().getId());
        dto.setTechnicianId(assignment.getTechnician().getId());
        dto.setTechnicianUsername(assignment.getTechnician().getUsername());
        dto.setTechnicianDisplayName(assignment.getTechnician().getDisplayName());
        dto.setAssignedById(assignment.getAssignedBy().getId());
        dto.setAssignedByUsername(assignment.getAssignedBy().getUsername());
        dto.setAssignedAt(assignment.getAssignedAt());
        return dto;
    }
}
