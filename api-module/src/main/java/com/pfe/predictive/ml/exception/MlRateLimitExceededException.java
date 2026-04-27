package com.pfe.predictive.ml.exception;

public class MlRateLimitExceededException extends RuntimeException {
    public MlRateLimitExceededException(String message) {
        super(message);
    }
}
