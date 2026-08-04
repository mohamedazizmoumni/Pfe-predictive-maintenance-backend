package com.pfe.predictive.ml.controller;

import com.pfe.predictive.core.entity.PredictionRecord;
import com.pfe.predictive.data.repository.PredictionRecordRepository;
import com.pfe.predictive.ml.dto.PredictionRecordDTO;
import com.pfe.predictive.ml.dto.PredictionRecordDetailDTO;
import com.pfe.predictive.ml.mapper.PredictionRecordMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/machines/{machineId}/predictions")
@Tag(name = "Prediction Records", description = "Query historical ML prediction results")
public class PredictionRecordsController {

    private final PredictionRecordRepository predictionRecordRepository;
    private final PredictionRecordMapper mapper;

    public PredictionRecordsController(PredictionRecordRepository predictionRecordRepository,
                                      PredictionRecordMapper mapper) {
        this.predictionRecordRepository = predictionRecordRepository;
        this.mapper = mapper;
    }

    /**
     * Get paginated list of predictions for a machine (most recent first).
     * 
     * @param machineId machine identifier
     * @param pageable pagination parameters (default: page=0, size=20, sorted by predictedAt DESC)
     * @return paginated prediction records
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'MANAGER', 'DATA_SCIENTIST', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "List prediction history for machine",
               description = "Returns paginated prediction records, most recent first")
    public ResponseEntity<Page<PredictionRecordDTO>> listPredictions(
            @PathVariable Long machineId,
            @PageableDefault(size = 20, sort = "predictedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<PredictionRecordDTO> records = predictionRecordRepository
                .findByMachineIdOrderByPredictedAtDesc(machineId, pageable)
                .map(mapper::toDTO);

        System.out.println("Retrieved " + records.getNumberOfElements() + " prediction records for machine " + machineId);
        return ResponseEntity.ok(records);
    }

    /**
     * Get the most recent prediction for a machine.
     * Useful for displaying current RUL status on dashboard.
     * 
     * @param machineId machine identifier
     * @return the most recent prediction record with full details
     */
    @GetMapping("/latest")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'MANAGER', 'DATA_SCIENTIST', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get latest prediction for machine",
               description = "Returns the most recent prediction record")
    public ResponseEntity<?> getLatestPrediction(@PathVariable Long machineId) {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 1);
        Page<?> page = predictionRecordRepository.findMostRecentByMachineId(machineId, pageable);

        if (page.isEmpty()) {
            return ResponseEntity.ok().build();
        }

        var record = page.getContent().get(0);
        return ResponseEntity.ok(mapper.toDetailDTO((PredictionRecord) record));
    }

    /**
     * Get daily average RUL trend for charting/visualization.
     * Useful for Angular dashboard to plot RUL over time.
     * 
     * @param machineId machine identifier
     * @param days number of days to look back (default: 30)
     * @return list of daily trend data
     */
    @GetMapping("/trend")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'MANAGER', 'DATA_SCIENTIST', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get RUL trend for charting",
               description = "Returns daily average RUL values for trend visualization")
    public ResponseEntity<List<DailyRulTrend>> getPredictionTrend(
            @PathVariable Long machineId,
            @RequestParam(defaultValue = "30") int days) {

        LocalDateTime fromDate = LocalDateTime.now().minusDays(days);
        LocalDateTime toDate = LocalDateTime.now();

        List<DailyRulTrend> trends = new ArrayList<>(predictionRecordRepository
                .findByMachineIdAndPredictedAtBetween(machineId, fromDate, toDate)
                .stream()
                .collect(Collectors.groupingBy(
                    r -> r.getPredictedAt().toLocalDate(),
                    Collectors.averagingDouble(r -> r.getRulValue())
                ))
                .entrySet()
                .stream()
                .sorted((a, b) -> a.getKey().compareTo(b.getKey()))
                .map(e -> new DailyRulTrend(e.getKey(), e.getValue()))
                .collect(Collectors.toList()));

        // DEBUG: Retrieved {} daily trend points for machine {} over {} days

        return ResponseEntity.ok(trends);
    }

    /**
     * Get predictions for a specific date range.
     * 
     * @param machineId machine identifier
     * @param fromDate start date (inclusive)
     * @param toDate end date (inclusive)
     * @param pageable pagination parameters
     * @return paginated predictions in date range
     */
    @GetMapping("/range")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'MANAGER', 'DATA_SCIENTIST', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get predictions in date range",
               description = "Returns predictions between fromDate and toDate")
    public ResponseEntity<Page<PredictionRecordDTO>> getPredictionsInRange(
            @PathVariable Long machineId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,
            @PageableDefault(size = 50, sort = "predictedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = toDate.atTime(LocalTime.MAX);

        Page<PredictionRecordDTO> page = predictionRecordRepository
                .findByMachineIdAndPredictedAtBetween(machineId, from, to, pageable)
                .map(mapper::toDTO);

        return ResponseEntity.ok(page);
    }

    /**
     * Get critical RUL predictions across fleet (for admin dashboards).
     * Useful for identifying machines requiring immediate attention.
     * 
     * @param pageable pagination parameters
     * @return paginated list of critical predictions
     */
    @GetMapping("/critical/all")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get all critical predictions",
               description = "Returns predictions with CRITICAL risk level across entire fleet")
    public ResponseEntity<Page<PredictionRecordDTO>> getCriticalPredictions(
            @PageableDefault(size = 20, sort = "predictedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<PredictionRecordDTO> page = predictionRecordRepository
                .findAllCriticalPredictions(pageable)
                .map(mapper::toDTO);

        return ResponseEntity.ok(page);
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @Schema(description = "Daily RUL average")
    public static class DailyRulTrend {
        @Schema(description = "Date", example = "2026-04-20")
        private LocalDate date;

        @Schema(description = "Average RUL for the day", example = "156.7")
        private Double averageRul;
        
        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
        public Double getAverageRul() { return averageRul; }
        public void setAverageRul(Double averageRul) { this.averageRul = averageRul; }
    }
}
