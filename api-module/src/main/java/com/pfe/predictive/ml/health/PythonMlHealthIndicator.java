package com.pfe.predictive.ml.health;

import com.pfe.predictive.ml.dto.ModelInfoResponse;
import com.pfe.predictive.ml.service.MlMetadataService;
import com.pfe.predictive.ml.service.PythonMlClient;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component("pythonMl")
public class PythonMlHealthIndicator implements HealthIndicator {

    private final PythonMlClient pythonMlClient;
    private final MlMetadataService mlMetadataService;

    public PythonMlHealthIndicator(PythonMlClient pythonMlClient,
                                   MlMetadataService mlMetadataService) {
        this.pythonMlClient = pythonMlClient;
        this.mlMetadataService = mlMetadataService;
    }

    @Override
    public Health health() {
        String correlationId = UUID.randomUUID().toString();
        try {
            Map<String, Object> health = pythonMlClient.health(correlationId);
            ModelInfoResponse info = mlMetadataService.getModelInfo(correlationId);

            boolean modelLoaded = Boolean.TRUE.equals(info.getModelLoaded());
            if (!modelLoaded) {
                return Health.down()
                        .withDetail("pythonHealth", health)
                        .withDetail("modelLoaded", false)
                        .withDetail("reason", "Python ML model is not loaded")
                        .build();
            }

            return Health.up()
                    .withDetail("pythonHealth", health)
                    .withDetail("featureCount", info.getFeatureCount())
                    .withDetail("modelLoaded", true)
                    .build();
        } catch (Exception ex) {
            return Health.down(ex)
                    .withDetail("reason", "Python ML service unavailable")
                    .build();
        }
    }
}
