package com.pfe.predictive.auth.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FaceRecognitionResult {
    @JsonAlias({"user_id", "id", "user"})
    private Long userId;

    @JsonAlias({"score", "similarity", "distance"})
    private Double confidence;

    public FaceRecognitionResult() {}

    public FaceRecognitionResult(Long userId, Double confidence) {
        this.userId = userId;
        this.confidence = confidence;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
}
