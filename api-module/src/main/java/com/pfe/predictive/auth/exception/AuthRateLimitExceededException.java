package com.pfe.predictive.auth.exception;

public class AuthRateLimitExceededException extends RuntimeException {
    public AuthRateLimitExceededException(String message) {
        super(message);
    }
}
