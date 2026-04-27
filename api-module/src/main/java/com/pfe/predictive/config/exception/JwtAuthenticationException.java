package com.pfe.predictive.config.exception;

public class JwtAuthenticationException extends RuntimeException {
    
    private final String errorCode;
    private final int httpStatus;
    
    public JwtAuthenticationException(String message, String errorCode, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
    
    public JwtAuthenticationException(String message, String errorCode, int httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public int getHttpStatus() {
        return httpStatus;
    }
}
