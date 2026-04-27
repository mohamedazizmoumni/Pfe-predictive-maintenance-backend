package com.pfe.predictive.ml.dto;

import java.util.List;

public class PredictionRequest {

    private List<List<Double>> features;

    public PredictionRequest() {
    }

    public PredictionRequest(List<List<Double>> features) {
        this.features = features;
    }

    public List<List<Double>> getFeatures() {
        return features;
    }

    public void setFeatures(List<List<Double>> features) {
        this.features = features;
    }
}
