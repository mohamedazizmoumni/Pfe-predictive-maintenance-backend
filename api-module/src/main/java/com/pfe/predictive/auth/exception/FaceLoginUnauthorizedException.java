package com.pfe.predictive.auth.exception;

public class FaceLoginUnauthorizedException extends RuntimeException {
    public FaceLoginUnauthorizedException(String message) {
        super(message);
    }
}
