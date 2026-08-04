package com.pfe.predictive.inventory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class InventoryExceptionHandler {

    /**
     * Let Spring's own NoResourceFoundException bubble up as a proper 404
     * instead of being swallowed by the catch-all below.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResource(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
            "timestamp", Instant.now().toString(),
            "status", 404,
            "error", "Not Found",
            "message", ex.getMessage() != null ? ex.getMessage() : "Resource not found"
        ));
    }

    /**
     * Catch-all for all other application exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAll(Exception ex) {
        // Re-raise ResponseStatusException so its status is preserved
        if (ex instanceof ResponseStatusException rse) {
            return ResponseEntity.status(rse.getStatusCode()).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", rse.getStatusCode().value(),
                "error", rse.getReason() != null ? rse.getReason() : "Error",
                "message", rse.getMessage()
            ));
        }
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
            "timestamp", Instant.now().toString(),
            "status", 500,
            "error", ex.getClass().getSimpleName(),
            "message", ex.getMessage() != null ? ex.getMessage() : "null",
            "cause", ex.getCause() != null ? ex.getCause().getMessage() : "none"
        ));
    }
}