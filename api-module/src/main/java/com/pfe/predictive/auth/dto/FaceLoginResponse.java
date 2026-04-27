package com.pfe.predictive.auth.dto;

public class FaceLoginResponse extends LoginResponse {
    private Double confidence;

    public FaceLoginResponse() {
        super();
    }

    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }

    public static FaceLoginResponse from(LoginResponse loginResponse, Double confidence) {
        FaceLoginResponse response = new FaceLoginResponse();
        response.setToken(loginResponse.getToken());
        response.setRefreshToken(loginResponse.getRefreshToken());
        response.setId(loginResponse.getId());
        response.setUsername(loginResponse.getUsername());
        response.setEmail(loginResponse.getEmail());
        response.setFirstName(loginResponse.getFirstName());
        response.setLastName(loginResponse.getLastName());
        response.setRoles(loginResponse.getRoles());
        response.setConfidence(confidence);
        return response;
    }
}
