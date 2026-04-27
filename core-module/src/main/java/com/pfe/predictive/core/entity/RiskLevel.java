package com.pfe.predictive.core.entity;

/**
 * Risk level classification for machine predictions.
 * Used by PredictionRecord to indicate the severity of predicted failure risk.
 */
public enum RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
