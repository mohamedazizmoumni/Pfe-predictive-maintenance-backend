package com.pfe.predictive.ml.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public class PredictionApiRequest {

    @NotNull(message = "features is required")
    private List<List<Double>> features;

    public List<List<Double>> getFeatures() {
        return features;
    }

    public void setFeatures(List<List<Double>> features) {
        this.features = features;
    }
}
