package com.yourpackage.business.controller;

import com.pfe.predictive.maintenancecost.entity.MaintenancePart;
import com.yourpackage.business.service.MaintenancePartService;
import jakarta.validation.Valid;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory/parts")
public class PartController {

    private final MaintenancePartService maintenancePartService;

    public PartController(MaintenancePartService maintenancePartService) {
        this.maintenancePartService = maintenancePartService;
    }

    @GetMapping
    public ResponseEntity<List<MaintenancePart>> getAllParts() {
        return ResponseEntity.ok(maintenancePartService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaintenancePart> getPartById(@PathVariable Long id) {
        MaintenancePart part = maintenancePartService.findById(id);
        return ResponseEntity.ok(part);
    }

    @PostMapping
    public ResponseEntity<MaintenancePart> createPart(@RequestBody @Valid MaintenancePart part) {
        MaintenancePart created = maintenancePartService.create(part);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MaintenancePart> updatePart(@PathVariable Long id, @RequestBody @Valid MaintenancePart part) {
        MaintenancePart existing = maintenancePartService.findById(id);
        existing.setName(part.getName());
        existing.setReferenceCode(part.getReferenceCode());
        existing.setUnitCost(part.getUnitCost());
        existing.setStockQuantity(part.getStockQuantity());
        existing.setLeadTimeDays(part.getLeadTimeDays());
        return ResponseEntity.ok(maintenancePartService.create(existing));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePart(@PathVariable Long id) {
        MaintenancePart existing = maintenancePartService.findById(id);
        maintenancePartService.delete(existing.getId());
        return ResponseEntity.noContent().build();
    }
}