package com.pfe.predictive.machine.controller;

import com.pfe.predictive.machine.dto.CreateMachineRequest;
import com.pfe.predictive.machine.dto.MachineDTO;
import com.pfe.predictive.machine.service.MachineCommandService;
import com.pfe.predictive.machine.service.MachineQueryService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    public MachineController(MachineQueryService machineQueryService,
                             MachineCommandService machineCommandService) {
        this.machineQueryService = machineQueryService;
        this.machineCommandService = machineCommandService;
    }

    @GetMapping
    public ResponseEntity<List<MachineDTO>> getAllMachines() {
        return ResponseEntity.ok(machineQueryService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MachineDTO> getMachineById(@PathVariable Long id) {
        return machineQueryService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MachineDTO> createMachine(@Valid @RequestBody CreateMachineRequest request) {
        return ResponseEntity.ok(machineQueryService.findById(
                machineCommandService.createMachine(request, null).getId()
        ).orElseThrow());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MachineDTO> createMachineWithPhoto(
            @Valid CreateMachineRequest request,
            @RequestPart(value = "photo", required = false) MultipartFile photo) {
        return ResponseEntity.ok(machineQueryService.findById(
                machineCommandService.createMachine(request, photo).getId()
        ).orElseThrow());
    }

    @GetMapping("/{id}/photo")
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
}