package com.pfe.predictive.inventory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

/**
 * Temporary diagnostic handler — exposes the real exception message in the
 * 500 response body so you can see what's failing without access to the
 * server console. Remove or restrict this before going to production.
 */
@Slf4j
@RestControllerAdvice
public class InventoryExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAll(Exception ex) {
        log.error("Unhandled exception in inventory module", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
            "timestamp", Instant.now().toString(),
            "status", 500,
            "error", ex.getClass().getSimpleName(),
            "message", ex.getMessage() != null ? ex.getMessage() : "null",
            "cause", ex.getCause() != null ? ex.getCause().getMessage() : "none"
        ));
    }
}