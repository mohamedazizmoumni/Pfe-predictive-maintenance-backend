package com.pfe.predictive.simulation;

import com.pfe.predictive.telemetry.replay.MachineTrajectoryManager;
import com.pfe.predictive.telemetry.replay.TelemetryReplayScheduler;
import com.pfe.predictive.telemetry.replay.model.MachineTrajectoryStatus;
import com.pfe.predictive.websocket.service.MachineStreamingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/simulation")
@Tag(name = "Telemetry Replay", description = "NASA C-MAPSS dataset replay control and monitoring")
public class SimulationController {

    private final TelemetryReplayScheduler replayScheduler;
    private final MachineTrajectoryManager trajectoryManager;
    private final MachineStreamingService streamingService;

    public SimulationController(
            TelemetryReplayScheduler replayScheduler,
            MachineTrajectoryManager trajectoryManager,
            MachineStreamingService streamingService) {
        this.replayScheduler = replayScheduler;
        this.trajectoryManager = trajectoryManager;
        this.streamingService = streamingService;
    }

    @GetMapping("/machines/{machineId}/state")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'MANAGER', 'DATA_SCIENTIST', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get machine replay status", description = "Returns the current NASA C-MAPSS replay state")
    public ResponseEntity<MachineTrajectoryStatus> getMachineState(@PathVariable Long machineId) {
        return ResponseEntity.ok(trajectoryManager.snapshot(machineId));
    }

    @GetMapping("/critical-machines")
    @PreAuthorize("hasAnyRole('MANAGER', 'DATA_SCIENTIST', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get critical machines", description = "Returns all machines in critical replay state")
    public ResponseEntity<List<MachineTrajectoryStatus>> getCriticalMachines() {
        List<MachineTrajectoryStatus> critical = trajectoryManager.getAllStatuses().stream()
                .filter(status -> status.failureDetected() || status.health() <= 30.0d)
                .toList();
        return ResponseEntity.ok(critical);
    }

    @GetMapping("/machines-by-health")
    @PreAuthorize("hasAnyRole('MANAGER', 'DATA_SCIENTIST', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get machines by health range")
    public ResponseEntity<Map<String, Long>> getMachinesByHealth() {
        List<MachineTrajectoryStatus> statuses = trajectoryManager.getAllStatuses();
        return ResponseEntity.ok(Map.of(
                "excellent", statuses.stream().filter(status -> status.health() >= 90.0d).count(),
                "good", statuses.stream().filter(status -> status.health() >= 70.0d && status.health() < 90.0d).count(),
                "fair", statuses.stream().filter(status -> status.health() >= 50.0d && status.health() < 70.0d).count(),
                "poor", statuses.stream().filter(status -> status.health() >= 30.0d && status.health() < 50.0d).count(),
                "critical", statuses.stream().filter(status -> status.health() < 30.0d).count()
        ));
    }

    @PostMapping("/trigger")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Manually trigger replay", description = "Runs one replay tick for all machines")
    public ResponseEntity<Map<String, String>> triggerReplay() {
        replayScheduler.triggerOnce();
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Replay tick triggered"
        ));
    }

    @PostMapping("/machines/{machineId}/simulate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Advance specific machine")
    public ResponseEntity<Map<String, String>> advanceMachine(@PathVariable Long machineId) {
        streamingService.streamSpecificMachine(machineId);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Machine replay advanced",
                "machineId", machineId.toString()
        ));
    }

    @PostMapping("/machines/{machineId}/pause")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Pause machine replay")
    public ResponseEntity<Map<String, String>> pauseMachine(@PathVariable Long machineId) {
        trajectoryManager.pause(machineId);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Machine replay paused",
                "machineId", machineId.toString()
        ));
    }

    @PostMapping("/machines/{machineId}/resume")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Resume machine replay")
    public ResponseEntity<Map<String, String>> resumeMachine(@PathVariable Long machineId) {
        trajectoryManager.resume(machineId);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Machine replay resumed",
                "machineId", machineId.toString()
        ));
    }

    @PostMapping("/machines/{machineId}/speed/{speed}")
    @PreAuthorize("hasAnyRole('MANAGER', 'DATA_SCIENTIST', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update replay speed")
    public ResponseEntity<Map<String, String>> setReplaySpeed(@PathVariable Long machineId, @PathVariable double speed) {
        trajectoryManager.setSpeed(machineId, speed);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Replay speed updated",
                "machineId", machineId.toString(),
                "speed", Double.toString(speed)
        ));
    }

    @PostMapping("/machines/{machineId}/reset")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Reset machine replay trajectory")
    public ResponseEntity<Map<String, String>> resetMachine(@PathVariable Long machineId) {
        trajectoryManager.reset(machineId);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Machine trajectory reset",
                "machineId", machineId.toString()
        ));
    }

    @PostMapping("/machines/{machineId}/maintenance")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Apply maintenance reset", description = "Resets the trajectory to healthy state after maintenance")
    public ResponseEntity<Map<String, String>> performMaintenance(@PathVariable Long machineId,
                                                                   @RequestParam(defaultValue = "PREDICTIVE") String maintenanceType) {
        trajectoryManager.reset(machineId);
        trajectoryManager.resume(machineId);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Replay trajectory reset after " + maintenanceType + " maintenance",
                "machineId", machineId.toString()
        ));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('MANAGER', 'DATA_SCIENTIST', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get replay statistics")
    public ResponseEntity<ReplayStats> getReplayStats() {
        List<MachineTrajectoryStatus> statuses = trajectoryManager.getAllStatuses();
        long active = statuses.stream().filter(MachineTrajectoryStatus::isActive).count();
        long paused = statuses.stream().filter(MachineTrajectoryStatus::paused).count();
        long completed = statuses.stream().filter(MachineTrajectoryStatus::completed).count();
        long critical = statuses.stream().filter(status -> status.failureDetected() || status.health() < 30.0d).count();
        double averageHealth = statuses.stream().mapToDouble(MachineTrajectoryStatus::health).average().orElse(0.0d);

        ReplayStats stats = new ReplayStats();
        stats.setAverageHealth(averageHealth);
        stats.setActiveMachineCount(active);
        stats.setPausedMachineCount(paused);
        stats.setCompletedMachineCount(completed);
        stats.setCriticalMachineCount(critical);
        stats.setTotalMachines(statuses.size());
        return ResponseEntity.ok(stats);
    }

    @Data
    public static class ReplayStats {
        private double averageHealth;
        private long activeMachineCount;
        private long pausedMachineCount;
        private long completedMachineCount;
        private long criticalMachineCount;
        private long totalMachines;
    }
}
