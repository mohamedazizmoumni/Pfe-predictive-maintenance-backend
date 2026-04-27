package com.pfe.predictive.ml.exception;

public class MlDependencyFailureException extends RuntimeException {
    public MlDependencyFailureException(String message) {
        super(message);
    }

    public MlDependencyFailureException(String message, Throwable cause) {
        super(message, cause);
    }
}
