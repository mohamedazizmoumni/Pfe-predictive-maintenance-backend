package com.pfe.predictive.ml.dto;

import java.util.List;

public class PredictionResponse {

    private List<Double> prediction;

    public List<Double> getPrediction() {
        return prediction;
    }

    public void setPrediction(List<Double> prediction) {
        this.prediction = prediction;
    }
}
