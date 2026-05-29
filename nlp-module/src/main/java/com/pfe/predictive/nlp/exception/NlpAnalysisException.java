package com.pfe.predictive.nlp.exception;

public class NlpAnalysisException extends RuntimeException {

    public NlpAnalysisException(String message) {
        super(message);
    }

    public NlpAnalysisException(String message, Throwable cause) {
        super(message, cause);
    }
}
