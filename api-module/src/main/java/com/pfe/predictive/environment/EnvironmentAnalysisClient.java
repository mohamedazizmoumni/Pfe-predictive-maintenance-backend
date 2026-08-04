package com.pfe.predictive.environment;

import com.pfe.predictive.environment.dto.EnvironmentAnalysisResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Client for ml_service's environment-risk analysis endpoint.
 *
 * Deliberately independent of MLServiceClient/the RUL model - environmental
 * readings (temperature/humidity) never feed the C-MAPSS-trained model, they
 * only drive this separate, decoupled risk assessment.
 */
@Service
@Slf4j
public class EnvironmentAnalysisClient {

    // Same thresholds as ml_service's EnvironmentAnalyzer fallback path, so
    // the risk level a caller sees doesn't change based on whether ml_service
    // happens to be reachable.
    private static final double CRITICAL_TEMP = 40.0;
    private static final double CRITICAL_HUMIDITY = 80.0;
    private static final double WARNING_TEMP = 35.0;
    private static final double WARNING_HUMIDITY = 70.0;

    private final RestTemplate restTemplate;
    private final String mlServiceUrl;

    public EnvironmentAnalysisClient(
            RestTemplate restTemplate,
            @Value("${ml.service.url:http://localhost:8000}") String mlServiceUrl) {
        this.restTemplate = restTemplate;
        this.mlServiceUrl = mlServiceUrl;
    }

    public EnvironmentAnalysisResponse analyze(Double temperature, Double humidity) {
        try {
            String url = mlServiceUrl + "/environment/analyze";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Double>> entity = new HttpEntity<>(
                    Map.of("temperature", temperature, "humidity", humidity), headers);

            EnvironmentAnalysisResponse response = restTemplate.postForObject(
                    url, entity, EnvironmentAnalysisResponse.class);

            if (response != null && response.getRiskLevel() != null) {
                return response;
            }

            log.warn("ml_service returned empty environment analysis, using fallback");
            return fallback(temperature, humidity);
        } catch (Exception e) {
            log.warn("Failed to reach ml_service for environment analysis, using fallback: {}", e.getMessage());
            return fallback(temperature, humidity);
        }
    }

    private EnvironmentAnalysisResponse fallback(Double temperature, Double humidity) {
        double temp = temperature != null ? temperature : 0.0;
        double humid = humidity != null ? humidity : 0.0;

        String riskLevel;
        List<String> recommendations;

        if (temp > CRITICAL_TEMP || humid > CRITICAL_HUMIDITY) {
            riskLevel = "CRITICAL";
            recommendations = List.of(
                    "High temperature and/or humidity detected",
                    "Inspect cooling and ventilation systems");
        } else if (temp > WARNING_TEMP || humid > WARNING_HUMIDITY) {
            riskLevel = "WARNING";
            recommendations = List.of("Elevated temperature/humidity - monitor conditions");
        } else {
            riskLevel = "NORMAL";
            recommendations = List.of("Environmental conditions within normal range");
        }

        return EnvironmentAnalysisResponse.builder()
                .riskLevel(riskLevel)
                .recommendations(recommendations)
                .build();
    }
}
