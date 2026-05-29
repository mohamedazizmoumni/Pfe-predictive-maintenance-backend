package com.pfe.predictive.ml.service;

import com.pfe.predictive.core.entity.PredictionRecord;
import com.pfe.predictive.core.entity.RiskLevel;
import com.pfe.predictive.data.repository.PredictionRecordRepository;
import com.pfe.predictive.ml.dto.PredictionRequest;
import com.pfe.predictive.ml.dto.PredictionResponse;
import com.pfe.predictive.ml.exception.MlBadRequestException;
import com.pfe.predictive.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MlPredictionService {

    private static final Logger log = LoggerFactory.getLogger(MlPredictionService.class);

    private final PythonMlClient pythonMlClient;
    private final MlMetadataService mlMetadataService;
    private final PredictionRecordRepository predictionRecordRepository;
    private final NotificationService notificationService;

    public MlPredictionService(PythonMlClient pythonMlClient,
                              MlMetadataService mlMetadataService,
                              PredictionRecordRepository predictionRecordRepository,
                              NotificationService notificationService) {
        this.pythonMlClient = pythonMlClient;
        this.mlMetadataService = mlMetadataService;
        this.predictionRecordRepository = predictionRecordRepository;
        this.notificationService = notificationService;
    }

    public PredictionResponse predict(Long machineId, List<List<Double>> features, String correlationId, String requestId) {
        long startNanos = System.nanoTime();
        int expectedFeatureCount = mlMetadataService.expectedFeatureCount(correlationId);
        validateFeatures(features, expectedFeatureCount);

        PredictionResponse response = pythonMlClient.predict(new PredictionRequest(features), correlationId, machineId);
        long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;
        int responseSize = response.getPrediction() != null ? response.getPrediction().size() : 0;

        log.info("event=ml_prediction requestId={} correlationId={} machineId={} expectedFeatureCount={} sampleCount={} latencyMs={} status=success responseSize={}",
                requestId,
                correlationId,
                machineId,
                expectedFeatureCount,
                features.size(),
                latencyMs,
                responseSize);

        return response;
    }

    /**
     * Predict and persist the result with audit trail.
     * 
     * This method wraps the standard predict() call and additionally:
     * 1. Extracts RUL value from prediction response
     * 2. Converts RUL to risk level
     * 3. Creates PredictionRecord with metadata
     * 4. Saves to database for audit trail
     * 5. Returns both prediction and record ID
     * 
     * @param machineId ID of machine being predicted
     * @param features normalized feature vectors
     * @param correlationId trace ID for logging
     * @param requestId unique request identifier
     * @param triggeredBy username of user who triggered prediction
     * @param inputFeaturesSummary optional JSON summary of key features
     * @return prediction record with both response and persisted ID
     */
    @Transactional
    public PredictionRecordWithResponse predictAndPersist(
            Long machineId,
            List<List<Double>> features,
            String correlationId,
            String requestId,
            String triggeredBy,
            String inputFeaturesSummary) {

        // Get prediction from ML service
        PredictionResponse mlResponse = predict(machineId, features, correlationId, requestId);

        // Extract RUL value (first value in prediction array)
        Double rulValue = (mlResponse.getPrediction() != null && !mlResponse.getPrediction().isEmpty())
            ? mlResponse.getPrediction().get(0)
            : 0.0;

        // Get model version from metadata service
        String modelVersion = mlMetadataService.modelVersion(correlationId);

        // Determine risk level from RUL
        RiskLevel riskLevel = determineRiskLevel(rulValue);

        // Create prediction record
        PredictionRecord record = PredictionRecord.builder()
                .machineId(machineId)
                .predictedAt(LocalDateTime.now())
                .rulValue(rulValue)
                .riskLevel(riskLevel)
                .modelVersion(modelVersion)
                .triggeredBy(triggeredBy)
                .inputFeaturesSummary(inputFeaturesSummary)
                .build();

        // Persist to database
        PredictionRecord savedRecord = predictionRecordRepository.save(record);

        log.info("event=prediction_persisted machineId={} recordId={} rul={} riskLevel={} modelVersion={} triggeredBy={}",
                machineId,
                savedRecord.getId(),
                rulValue,
                riskLevel,
                modelVersion,
                triggeredBy);

        // Trigger notifications for HIGH and CRITICAL risks
        notificationService.notifyIfRequired(machineId, rulValue, riskLevel, savedRecord.getId());

        return new PredictionRecordWithResponse(mlResponse, savedRecord);
    }

    /**
     * Determine risk level based on RUL value.
     * 
     * Thresholds (hours):
     * - CRITICAL: RUL < 24 hours (1 day)
     * - HIGH: 24-72 hours (1-3 days)
     * - MEDIUM: 72-168 hours (3-7 days)
     * - LOW: RUL > 168 hours (> 7 days)
     */
    private RiskLevel determineRiskLevel(Double rulValue) {
        if (rulValue == null || rulValue < 0) {
            return RiskLevel.CRITICAL;
        }
        if (rulValue < 24) {
            return RiskLevel.CRITICAL;
        } else if (rulValue < 72) {
            return RiskLevel.HIGH;
        } else if (rulValue < 168) {
            return RiskLevel.MEDIUM;
        } else {
            return RiskLevel.LOW;
        }
    }

    private void validateFeatures(List<List<Double>> features, int expectedFeatureCount) {
        if (features == null || features.isEmpty()) {
            throw new MlBadRequestException("features must contain at least one sample");
        }

        for (int rowIndex = 0; rowIndex < features.size(); rowIndex++) {
            List<Double> sample = features.get(rowIndex);
            if (sample == null) {
                throw new MlBadRequestException("features[" + rowIndex + "] cannot be null");
            }
            if (sample.size() != expectedFeatureCount) {
                throw new MlBadRequestException("Each sample must contain exactly " + expectedFeatureCount + " features; found " + sample.size() + " at index " + rowIndex);
            }

            for (int colIndex = 0; colIndex < sample.size(); colIndex++) {
                Double value = sample.get(colIndex);
                if (value == null || !Double.isFinite(value)) {
                    throw new MlBadRequestException("features[" + rowIndex + "][" + colIndex + "] must be a finite numeric value");
                }
            }
        }
    }

    /**
     * Helper class combining ML prediction response with persisted record ID.
     */
    public static class PredictionRecordWithResponse {
        public final PredictionResponse prediction;
        public final PredictionRecord record;

        public PredictionRecordWithResponse(PredictionResponse prediction, PredictionRecord record) {
            this.prediction = prediction;
            this.record = record;
        }

        public PredictionResponse getPrediction() {
            return prediction;
        }

        public PredictionRecord getRecord() {
            return record;
        }

        public Long getRecordId() {
            return record.getId();
        }
    }
}
