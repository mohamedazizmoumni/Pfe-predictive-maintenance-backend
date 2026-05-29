package com.pfe.predictive.ml.service;

import com.pfe.predictive.core.entity.Machine;
import com.pfe.predictive.core.entity.SensorTelemetry;
import com.pfe.predictive.data.repository.MachineRepository;
import com.pfe.predictive.data.repository.SensorTelemetryRepository;
import com.pfe.predictive.ml.dto.MLFeatures;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Function;

/**
 * LAYER 4: FEATURE ENGINEERING SERVICE
 * 
 * Converts raw 24h sensor data into ML-ready features.
 * 
 * Computations:
 * - Rolling averages (24h window)
 * - Trends (linear regression slopes)
 * - Acceleration (trend changes)
 * - Statistical measures (max, min, stddev)
 * - Operational metrics
 * 
 * Output: 19 engineered features (NOT raw sensors!)
 */
@Service
@Slf4j
public class FeatureEngineeringService {
    
    private final SensorTelemetryRepository telemetryRepository;
    private final MachineRepository machineRepository;
    
    private static final int HOURS_WINDOW = 24;
    
    public FeatureEngineeringService(
            SensorTelemetryRepository telemetryRepository,
            MachineRepository machineRepository) {
        this.telemetryRepository = telemetryRepository;
        this.machineRepository = machineRepository;
    }
    
    /**
     * Engineer ML features from last 24 hours of sensor data.
     * 
     * @param machineId Machine ID
     * @return MLFeatures with 19 engineered features
     * @throws IllegalStateException if insufficient data
     */
    public MLFeatures engineerFeatures(Long machineId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime last24h = now.minusHours(HOURS_WINDOW);
        
        // Fetch raw sensor data
        List<SensorTelemetry> telemetry = telemetryRepository
                .findByMachineIdAndTimestampBetween(machineId, last24h, now);
        
        if (telemetry.isEmpty()) {
            throw new IllegalStateException(
                String.format("No sensor data in last %d hours for machine %d", HOURS_WINDOW, machineId)
            );
        }
        
        if (telemetry.size() < 10) {
            log.warn("Insufficient data for machine {}: only {} readings in last {}h", 
                machineId, telemetry.size(), HOURS_WINDOW);
        }
        
        // Get machine metadata
        Machine machine = machineRepository.findById(machineId)
                .orElseThrow(() -> new IllegalArgumentException("Machine not found: " + machineId));
        
        // Build features
        MLFeatures features = MLFeatures.builder()
                // Temperature features
                .temperatureAvg(calculateAverage(telemetry, SensorTelemetry::getSensor1))
                .temperatureMax(calculateMax(telemetry, SensorTelemetry::getSensor1))
                .temperatureMin(calculateMin(telemetry, SensorTelemetry::getSensor1))
                .temperatureStdDev(calculateStdDev(telemetry, SensorTelemetry::getSensor1))
                .temperatureTrend(calculateTrend(telemetry, SensorTelemetry::getSensor1))
                .temperatureAcceleration(calculateAcceleration(telemetry, SensorTelemetry::getSensor1))
                
                // Vibration features
                .vibrationAvg(calculateAverage(telemetry, SensorTelemetry::getSensor2))
                .vibrationMax(calculateMax(telemetry, SensorTelemetry::getSensor2))
                .vibrationTrend(calculateTrend(telemetry, SensorTelemetry::getSensor2))
                .vibrationAcceleration(calculateAcceleration(telemetry, SensorTelemetry::getSensor2))
                
                // Power features
                .powerAvg(calculateAverage(telemetry, SensorTelemetry::getSensor3))
                .powerTrend(calculateTrend(telemetry, SensorTelemetry::getSensor3))
                
                // Pressure features
                .pressureAvg(calculateAverage(telemetry, SensorTelemetry::getSensor4))
                .pressureTrend(calculateTrend(telemetry, SensorTelemetry::getSensor4))
                
                .build();
        
        // Operational features
        features.setOperatingHours(machine.getOperatingHours() != null ? machine.getOperatingHours().longValue() : 0L);
        features.setDaysSinceLastMaintenance(
            machine.getLastMaintenanceDate() != null
                ? ChronoUnit.DAYS.between(machine.getLastMaintenanceDate(), now)
                : 365L
        );
        features.setCyclesSinceMaintenance(telemetry.size());  // Approximate
        
        // Derived features
        features.setTemperatureVibrationRatio(
            features.getTemperatureAvg() / Math.max(1.0, features.getVibrationAvg())
        );
        features.setPowerEfficiency(
            Math.max(0.0, 100.0 - (features.getPowerAvg() - 10.0) * 2.0)  // Simplified
        );
        
        log.info("Engineered {} features for machine {} from {} readings",
            MLFeatures.getFeatureCount(), machineId, telemetry.size());
        
        return features;
    }
    
    // ==================== STATISTICAL CALCULATIONS ====================
    
    private double calculateAverage(List<SensorTelemetry> data, Function<SensorTelemetry, Double> extractor) {
        return data.stream()
                .map(extractor)
                .filter(v -> v != null)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }
    
    private double calculateMax(List<SensorTelemetry> data, Function<SensorTelemetry, Double> extractor) {
        return data.stream()
                .map(extractor)
                .filter(v -> v != null)
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0.0);
    }
    
    private double calculateMin(List<SensorTelemetry> data, Function<SensorTelemetry, Double> extractor) {
        return data.stream()
                .map(extractor)
                .filter(v -> v != null)
                .mapToDouble(Double::doubleValue)
                .min()
                .orElse(0.0);
    }
    
    private double calculateStdDev(List<SensorTelemetry> data, Function<SensorTelemetry, Double> extractor) {
        double avg = calculateAverage(data, extractor);
        
        double variance = data.stream()
                .map(extractor)
                .filter(v -> v != null)
                .mapToDouble(v -> Math.pow(v - avg, 2))
                .average()
                .orElse(0.0);
        
        return Math.sqrt(variance);
    }
    
    /**
     * Calculate trend using linear regression.
     * Returns slope (change per hour).
     */
    private double calculateTrend(List<SensorTelemetry> data, Function<SensorTelemetry, Double> extractor) {
        if (data.size() < 2) return 0.0;
        
        int n = data.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        
        for (int i = 0; i < n; i++) {
            double x = i;  // Time index
            Double value = extractor.apply(data.get(i));
            if (value == null) continue;
            
            double y = value;
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }
        
        // Linear regression: y = mx + b, we want m (slope)
        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        
        // Convert to per-hour rate (assuming data points are evenly spaced)
        double hoursPerDataPoint = (double) HOURS_WINDOW / n;
        return slope / hoursPerDataPoint;
    }
    
    /**
     * Calculate acceleration (change in trend rate).
     * Compares recent trend (last 25%) vs historical trend (first 75%).
     */
    private double calculateAcceleration(List<SensorTelemetry> data, Function<SensorTelemetry, Double> extractor) {
        if (data.size() < 4) return 0.0;
        
        int splitPoint = (int) (data.size() * 0.75);
        
        List<SensorTelemetry> historical = data.subList(0, splitPoint);
        List<SensorTelemetry> recent = data.subList(splitPoint, data.size());
        
        double historicalTrend = calculateTrend(historical, extractor);
        double recentTrend = calculateTrend(recent, extractor);
        
        // Positive acceleration = trend is increasing (degradation accelerating)
        return recentTrend - historicalTrend;
    }
}
