package com.pfe.predictive.machine.controller;

import com.pfe.predictive.machine.dto.AssignTechnicianRequest;
import com.pfe.predictive.machine.dto.CreateMachineRequest;
import com.pfe.predictive.machine.dto.MachineDTO;
import com.pfe.predictive.machine.dto.MachineTechnicianDTO;
import com.pfe.predictive.machine.service.MachineAssignmentService;
import com.pfe.predictive.machine.service.MachineCommandService;
import com.pfe.predictive.machine.service.MachineQueryService;
import com.pfe.predictive.security.PermissionConstants;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/machines")
public class MachineController {

    private final MachineQueryService machineQueryService;
    private final MachineCommandService machineCommandService;
    private final MachineAssignmentService machineAssignmentService;

    public MachineController(MachineQueryService machineQueryService,
                             MachineCommandService machineCommandService,
                             MachineAssignmentService machineAssignmentService) {
        this.machineQueryService = machineQueryService;
        this.machineCommandService = machineCommandService;
        this.machineAssignmentService = machineAssignmentService;
    }

    @GetMapping
    @PreAuthorize(PermissionConstants.PERM_MACHINE_READ)
    public ResponseEntity<List<MachineDTO>> getAllMachines() {
        return ResponseEntity.ok(machineQueryService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize(PermissionConstants.PERM_MACHINE_READ)
    public ResponseEntity<MachineDTO> getMachineById(@PathVariable Long id) {
        return machineQueryService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize(PermissionConstants.PERM_MACHINE_CREATE)
    public ResponseEntity<MachineDTO> createMachine(@Valid @RequestBody CreateMachineRequest request) {
        return ResponseEntity.ok(machineQueryService.findById(
                machineCommandService.createMachine(request, null).getId()
        ).orElseThrow());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(PermissionConstants.PERM_MACHINE_CREATE)
    public ResponseEntity<MachineDTO> createMachineWithPhoto(
            @Valid CreateMachineRequest request,
            @RequestPart(value = "photo", required = false) MultipartFile photo) {
        return ResponseEntity.ok(machineQueryService.findById(
                machineCommandService.createMachine(request, photo).getId()
        ).orElseThrow());
    }

    @GetMapping("/{id}/photo")
    @PreAuthorize(PermissionConstants.PERM_MACHINE_READ)
    public ResponseEntity<Resource> downloadPhoto(@PathVariable Long id) {
        MachineCommandService.PhotoDownload download = machineCommandService.loadPhoto(id);
        if (download == null) {
            return ResponseEntity.notFound().build();
        }

        String contentType = download.getContentType() == null || download.getContentType().isBlank()
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : download.getContentType();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(download.getResource());
    }

    @PostMapping("/{machineId}/technicians")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<MachineTechnicianDTO> assignTechnician(@PathVariable Long machineId,
            @Valid @RequestBody AssignTechnicianRequest request, Authentication authentication) {
        MachineTechnicianDTO dto = machineAssignmentService.assign(
                machineId, request.getTechnicianId(), authentication.getName());
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{machineId}/technicians/{technicianId}")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<Void> unassignTechnician(@PathVariable Long machineId, @PathVariable Long technicianId) {
        machineAssignmentService.unassign(machineId, technicianId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{machineId}/technicians")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<List<MachineTechnicianDTO>> listTechnicians(@PathVariable Long machineId) {
        return ResponseEntity.ok(machineAssignmentService.listForMachine(machineId));
    }
}