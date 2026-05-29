package com.pfe.predictive.ml.dto;

import lombok.*;

/**
 * LAYER 4: ML FEATURES DTO
 * 
 * Machine Health Vector - 19 engineered features for ML prediction.
 * 
 * These are NOT raw sensor values - they are aggregated statistics
 * computed from 24 hours of sensor data.
 * 
 * Features include:
 * - Rolling averages
 * - Trends (linear regression slopes)
 * - Acceleration (rate of change of trends)
 * - Statistical measures (max, min, stddev)
 * - Operational metrics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MLFeatures {
    
    // ==================== TEMPERATURE FEATURES (6) ====================
    
    /**
     * Average temperature over 24h (°F)
     */
    private double temperatureAvg;
    
    /**
     * Maximum temperature over 24h (°F)
     */
    private double temperatureMax;
    
    /**
     * Minimum temperature over 24h (°F)
     */
    private double temperatureMin;
    
    /**
     * Standard deviation of temperature over 24h
     */
    private double temperatureStdDev;
    
    /**
     * Temperature trend (°F/hour) - linear regression slope
     */
    private double temperatureTrend;
    
    /**
     * Temperature acceleration (change in trend rate)
     * Positive = accelerating increase
     */
    private double temperatureAcceleration;
    
    // ==================== VIBRATION FEATURES (4) ====================
    
    /**
     * Average vibration over 24h (mm/s)
     */
    private double vibrationAvg;
    
    /**
     * Maximum vibration over 24h (mm/s)
     */
    private double vibrationMax;
    
    /**
     * Vibration trend (mm/s/hour)
     */
    private double vibrationTrend;
    
    /**
     * Vibration acceleration
     */
    private double vibrationAcceleration;
    
    // ==================== POWER FEATURES (2) ====================
    
    /**
     * Average power consumption over 24h (kW)
     */
    private double powerAvg;
    
    /**
     * Power consumption trend (kW/hour)
     */
    private double powerTrend;
    
    // ==================== PRESSURE FEATURES (2) ====================
    
    /**
     * Average pressure over 24h (PSI)
     */
    private double pressureAvg;
    
    /**
     * Pressure trend (PSI/hour)
     */
    private double pressureTrend;
    
    // ==================== OPERATIONAL FEATURES (3) ====================
    
    /**
     * Total operating hours
     */
    private long operatingHours;
    
    /**
     * Days since last maintenance
     */
    private long daysSinceLastMaintenance;
    
    /**
     * Number of operational cycles since maintenance
     */
    private int cyclesSinceMaintenance;
    
    // ==================== DERIVED FEATURES (2) ====================
    
    /**
     * Temperature to vibration ratio
     * Helps identify friction vs thermal issues
     */
    private double temperatureVibrationRatio;
    
    /**
     * Power efficiency estimate (%)
     * Based on power consumption relative to baseline
     */
    private double powerEfficiency;
    
    // ==================== CONVERSION TO ARRAY ====================
    
    /**
     * Convert to array for ML model input.
     * Order must match training data!
     * 
     * @return 19-element array
     */
    public double[] toArray() {
        return new double[] {
            temperatureAvg,
            temperatureMax,
            temperatureMin,
            temperatureStdDev,
            temperatureTrend,
            temperatureAcceleration,
            vibrationAvg,
            vibrationMax,
            vibrationTrend,
            vibrationAcceleration,
            powerAvg,
            powerTrend,
            pressureAvg,
            pressureTrend,
            (double) operatingHours,
            (double) daysSinceLastMaintenance,
            (double) cyclesSinceMaintenance,
            temperatureVibrationRatio,
            powerEfficiency
        };
    }
    
    /**
     * Get feature count.
     */
    public static int getFeatureCount() {
        return 19;
    }
    
    /**
     * Get feature names (for debugging/logging).
     */
    public static String[] getFeatureNames() {
        return new String[] {
            "temperatureAvg", "temperatureMax", "temperatureMin",
            "temperatureStdDev", "temperatureTrend", "temperatureAcceleration",
            "vibrationAvg", "vibrationMax", "vibrationTrend", "vibrationAcceleration",
            "powerAvg", "powerTrend",
            "pressureAvg", "pressureTrend",
            "operatingHours", "daysSinceLastMaintenance", "cyclesSinceMaintenance",
            "temperatureVibrationRatio", "powerEfficiency"
        };
    }
}
