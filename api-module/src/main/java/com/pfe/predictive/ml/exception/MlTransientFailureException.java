package com.pfe.predictive.ml.exception;

public class MlTransientFailureException extends RuntimeException {
    public MlTransientFailureException(String message) {
        super(message);
    }

    public MlTransientFailureException(String message, Throwable cause) {
        super(message, cause);
    }
}
