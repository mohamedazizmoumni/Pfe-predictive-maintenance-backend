package com.pfe.predictive.timeline.controller;

import com.pfe.predictive.timeline.dto.TimelineEntry;
import com.pfe.predictive.timeline.service.TimelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/machines/{machineId}/timeline")
@RequiredArgsConstructor
public class TimelineController {

    private final TimelineService timelineService;

    @GetMapping
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<TimelineEntry>> forMachine(@PathVariable Long machineId) {
        return ResponseEntity.ok(timelineService.forMachine(machineId));
    }
}
