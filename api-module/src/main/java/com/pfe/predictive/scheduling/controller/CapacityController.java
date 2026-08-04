package com.pfe.predictive.scheduling.controller;

import com.pfe.predictive.scheduling.dto.TechnicianCapacityDto;
import com.pfe.predictive.scheduling.service.CapacityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/scheduling/capacity")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'SUPER_ADMIN')")
@Tag(name = "Capacity Planning", description = "Per-technician open workload for scheduling decisions")
public class CapacityController {

    private final CapacityService capacityService;

    @GetMapping
    @Operation(summary = "Get open-job workload per technician, busiest first")
    public ResponseEntity<List<TechnicianCapacityDto>> getFleetCapacity() {
        return ResponseEntity.ok(capacityService.getFleetCapacity());
    }
}
