package com.pfe.predictive.ml.service;

import com.pfe.predictive.ml.dto.BatchPredictionResponse;
import com.pfe.predictive.ml.dto.MLPredictionResponse;
import com.pfe.predictive.ml.dto.MlPredictionRequest;
import com.pfe.predictive.ml.dto.MlTelemetryRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ML Service Client
 * 
 * Communicates with Python ML service for predictions and anomaly detection.
 * 
 * Endpoint: POST http://localhost:8000/predict
 * 
 * Handles:
 * - Failure prediction
 * - Anomaly detection
 * - RUL estimation
 * - Risk assessment
 * 
 * Key Features:
 * - Value clamping for all probability/confidence scores (0.0-1.0 range)
 * - Robust fallback when ML service unavailable
 * - Sanitization of NaN/Infinity values
 * - Contract validation to ensure no null/invalid payloads sent
 */
@Service
@Slf4j
public class MLServiceClient {
    
    private final RestTemplate restTemplate;
    private final String mlServiceUrl;
    
    public MLServiceClient(
            RestTemplate restTemplate,
            @Value("${ml.service.url:http://localhost:8000}") String mlServiceUrl) {
        this.restTemplate = restTemplate;
        this.mlServiceUrl = mlServiceUrl;
    }
    
    public MLPredictionResponse getPrediction(Long machineId, MlPredictionRequest request) {
        try {
            MlPredictionRequest sanitizedRequest = sanitizeRequest(request);
            
            String url = mlServiceUrl + "/predict";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<MlPredictionRequest> entity = new HttpEntity<>(sanitizedRequest, headers);
            
            log.debug("Calling ML service for machine {}: {}", machineId, url);
            
            ResponseEntity<BatchPredictionResponse> response = restTemplate.postForEntity(
                url,
                entity,
                BatchPredictionResponse.class
            );
            
            BatchPredictionResponse batchResponse = response.getBody();
            
            if (batchResponse != null && batchResponse.getPredictions() != null && !batchResponse.getPredictions().isEmpty()) {
                MLPredictionResponse prediction = batchResponse.getPredictions().get(0);
                
                // Sanitize and clamp all response values
                MLPredictionResponse sanitizedPrediction = sanitizePredictionResponse(prediction);
                
                log.debug("ML prediction received for machine {}: RUL={}, anomaly={}, risk={}",
                    machineId,
                    sanitizedPrediction.getPredictedRUL(),
                    sanitizedPrediction.getAnomalyProbability(),
                    sanitizedPrediction.getRiskLevel());
                
                return sanitizedPrediction;
            } else {
                log.warn("ML service returned empty response for machine {}", machineId);
                return createFallbackPrediction(machineId, sanitizedRequest);
            }
            
        } catch (Exception e) {
            log.error("Failed to get ML prediction for machine {}: {}",
                machineId, e.getMessage());
            
            return createFallbackPrediction(machineId, request);
        }
    }
    
    private MlPredictionRequest sanitizeRequest(MlPredictionRequest request) {
        if (request == null || request.getTelemetry() == null || request.getTelemetry().isEmpty()) {
            return MlPredictionRequest.builder()
                .telemetry(List.of(MlTelemetryRecord.builder()
                    .machineId(0L)
                    .timestamp(LocalDateTime.now().toString())
                    .cycle(0)
                    .sensors(List.of(0.0d))
                    .build()))
                    .build();
        }

        List<MlTelemetryRecord> sanitizedTelemetry = request.getTelemetry().stream()
                .map(record -> MlTelemetryRecord.builder()
                .machineId(record != null && record.getMachineId() != null ? record.getMachineId() : 0L)
                .timestamp(record != null && record.getTimestamp() != null ? record.getTimestamp() : LocalDateTime.now().toString())
                        .cycle(record != null && record.getCycle() != null ? Math.max(0, record.getCycle()) : 0)
                        .sensors(sanitizeSensors(record != null ? record.getSensors() : null))
                        .build())
                .toList();

        return MlPredictionRequest.builder()
                .telemetry(sanitizedTelemetry)
                .build();
    }

    private List<Double> sanitizeSensors(List<Double> sensors) {
        if (sensors == null || sensors.isEmpty()) {
            return List.of(0.0d);
        }
        return sensors.stream().map(this::safe).toList();
    }
    
    /**
     * Sanitize ML prediction response.
     * Clamps all probability/confidence values to [0.0, 1.0] range.
     * Ensures NO null values, NO NaN, NO Infinity.
     */
    private MLPredictionResponse sanitizePredictionResponse(MLPredictionResponse prediction) {
        return MLPredictionResponse.builder()
                .machineId(prediction.getMachineId())
                .timestamp(prediction.getTimestamp())
                // Core Predictions - clamp probabilities to [0.0, 1.0]
                .predictedRUL(safe(prediction.getPredictedRUL()))
                .anomalyProbability(clamp(prediction.getAnomalyProbability()))
                .failureProbability(clamp(prediction.getFailureProbability()))
                .riskLevel(prediction.getRiskLevel() != null ? prediction.getRiskLevel() : "UNKNOWN")
                // Classification
                .predictedFailureType(prediction.getPredictedFailureType() != null ? prediction.getPredictedFailureType() : "UNKNOWN")
                .anomalyType(prediction.getAnomalyType() != null ? prediction.getAnomalyType() : "NONE")
                // Actionable Insights - clamp severity and confidence
                .recommendedAction(prediction.getRecommendedAction() != null ? prediction.getRecommendedAction() : "Monitor system")
                .requiresImmediateAction(prediction.getRequiresImmediateAction() != null ? prediction.getRequiresImmediateAction() : false)
                .severity(clamp(prediction.getSeverity()))
                .confidenceScore(clamp(prediction.getConfidenceScore()))
                // Explainability
                .explanation(prediction.getExplanation())
                // Metadata
                .modelVersion(prediction.getModelVersion() != null ? prediction.getModelVersion() : "unknown")
                .processingTimeMs(safe(prediction.getProcessingTimeMs()))
                .build();
    }
    
    /**
     * Clamp value to [0.0, 1.0] range for probabilities and confidence scores.
     * Handles null, NaN, and Infinity values.
     * 
     * @param value the value to clamp
     * @return clamped value in [0.0, 1.0] range
     */
    private Double clamp(Double value) {
        if (value == null || Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }
    
    /**
     * Sanitize numeric value - replace null, NaN, Infinity with 0.0.
     * Used for sensor readings and other numeric fields.
     * 
     * @param value the value to sanitize
     * @return sanitized value (0.0 if invalid)
     */
    private Double safe(Double value) {
        if (value == null || Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        return value;
    }
    
    private MLPredictionResponse createFallbackPrediction(Long machineId, MlPredictionRequest request) {
        MlPredictionRequest sanitizedRequest = sanitizeRequest(request);
        MlTelemetryRecord firstRecord = sanitizedRequest.getTelemetry().get(0);
        List<Double> sensors = firstRecord.getSensors();

        double primarySignal = sensors.isEmpty() ? 0.0d : sensors.get(0);
        double normalizedSignal = Math.max(0.0d, Math.min(1.0d, primarySignal / 100.0d));
        Double health = 100.0d - (normalizedSignal * 80.0d);
        Double riskScore = 100.0d - health;
        
        String riskLevel;
        String recommendedAction;
        Boolean requiresImmediateAction = false;
        Double severity;
        Double anomalyProbability;
        
        // Rule-based risk assessment
        if (health < 30) {
            riskLevel = "CRITICAL";
            recommendedAction = "Schedule emergency maintenance immediately";
            requiresImmediateAction = true;
            severity = 0.9;
            anomalyProbability = 0.85;
        } else if (health < 50) {
            riskLevel = "HIGH";
            recommendedAction = "Schedule maintenance within 24 hours";
            severity = 0.7;
            anomalyProbability = 0.65;
        } else if (health < 70) {
            riskLevel = "MEDIUM";
            recommendedAction = "Monitor closely and schedule preventive maintenance";
            severity = 0.5;
            anomalyProbability = 0.4;
        } else {
            riskLevel = "LOW";
            recommendedAction = "Continue normal operations";
            severity = 0.2;
            anomalyProbability = 0.1;
        }
        
        // Estimate RUL based on health (bounded to reasonable range)
        Double predictedRUL = Math.max(0.0, (health / 100.0) * 200.0);
        
        // Estimate failure probability based on risk (clamped to [0.0, 1.0])
        Double failureProbability = clamp(riskScore / 100.0);
        
        String explanation = "Fallback prediction based on health metrics (ML service unavailable); "
            + "health=" + String.format("%.2f", health)
            + ", riskScore=" + String.format("%.2f", riskScore);
        
        return MLPredictionResponse.builder()
            .machineId(machineId)
            .timestamp(LocalDateTime.now().toString())
                .predictedRUL(predictedRUL)
                .anomalyProbability(anomalyProbability)
                .riskLevel(riskLevel)
                .failureProbability(failureProbability)
                .predictedFailureType("GENERAL_DEGRADATION")
                .recommendedAction(recommendedAction)
                .confidenceScore(0.6) // Lower confidence for fallback
                .anomalyType(health < 50 ? "DEGRADATION" : "NONE")
                .severity(severity)
                .requiresImmediateAction(requiresImmediateAction)
                .explanation(explanation)
                .modelVersion("fallback-1.0")
                .build();
    }
    
    /**
     * Check if ML service is available.
     */
    public boolean isMLServiceAvailable() {
        try {
            String url = mlServiceUrl + "/health";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.debug("ML service not available: {}", e.getMessage());
            return false;
        }
    }
}

