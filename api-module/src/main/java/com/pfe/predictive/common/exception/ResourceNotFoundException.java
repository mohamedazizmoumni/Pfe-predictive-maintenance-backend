package com.pfe.predictive.common.exception;

/**
 * Resource Not Found Exception
 * Thrown when a requested resource does not exist
 * 
 * @author Predictive Maintenance System
 * @version 1.0
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
