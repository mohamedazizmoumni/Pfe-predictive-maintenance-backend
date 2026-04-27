package com.pfe.predictive.ml.service;

import com.pfe.predictive.ml.config.MlServiceProperties;
import com.pfe.predictive.ml.dto.ModelInfoResponse;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicReference;

@Service
public class MlMetadataService {

    private final PythonMlClient pythonMlClient;
    private final MlServiceProperties properties;
    private final AtomicReference<ModelInfoResponse> cache = new AtomicReference<>();

    public MlMetadataService(PythonMlClient pythonMlClient, MlServiceProperties properties) {
        this.pythonMlClient = pythonMlClient;
        this.properties = properties;
    }

    public ModelInfoResponse getModelInfo(String correlationId) {
        ModelInfoResponse cached = cache.get();
        if (cached != null) {
            return cached;
        }
        ModelInfoResponse loaded = pythonMlClient.modelInfo(correlationId);
        cache.set(loaded);
        return loaded;
    }

    public ModelInfoResponse refresh(String correlationId) {
        ModelInfoResponse loaded = pythonMlClient.modelInfo(correlationId);
        cache.set(loaded);
        return loaded;
    }

    public int expectedFeatureCount(String correlationId) {
        ModelInfoResponse info = getModelInfo(correlationId);
        if (info.getFeatureCount() != null) {
            return info.getFeatureCount();
        }
        return properties.getExpectedFeatureCount();
    }

    public String modelVersion(String correlationId) {
        ModelInfoResponse info = getModelInfo(correlationId);
        if (info.getVersion() != null) {
            return info.getVersion();
        }
        return "unknown";
    }
}
