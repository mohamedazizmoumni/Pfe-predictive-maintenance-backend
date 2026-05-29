package com.pfe.predictive.nlp.exception;

public class NlpClientException extends NlpAnalysisException {

    public NlpClientException(String message) {
        super(message);
    }

    public NlpClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
