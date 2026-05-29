package com.pfe.predictive.common.exception;

import com.pfe.predictive.auth.exception.FaceLoginUnauthorizedException;
import com.pfe.predictive.auth.exception.FaceNotDetectedException;
import com.pfe.predictive.ml.exception.MlBadRequestException;
import com.pfe.predictive.ml.exception.MlDependencyFailureException;
import com.pfe.predictive.ml.exception.MlRateLimitExceededException;
import com.pfe.predictive.ml.exception.MlServiceUnavailableException;
import com.pfe.predictive.nlp.exception.NlpAnalysisNotFoundException;
import com.pfe.predictive.nlp.exception.NlpClientException;
import com.pfe.predictive.common.exception.ResourceNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {

        Map<String, Object> body = baseBody(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEntityNotFound(
            EntityNotFoundException ex,
            HttpServletRequest request) {

        Map<String, Object> body = baseBody(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(FaceNotDetectedException.class)
    public ResponseEntity<Map<String, Object>> handleFaceNotDetected(
            FaceNotDetectedException ex,
            HttpServletRequest request) {

        Map<String, Object> body = baseBody(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(FaceLoginUnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleFaceLoginUnauthorized(
            FaceLoginUnauthorizedException ex,
            HttpServletRequest request) {

        Map<String, Object> body = baseBody(HttpStatus.UNAUTHORIZED, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(MlBadRequestException.class)
    public ResponseEntity<Map<String, Object>> handleMlBadRequest(
            MlBadRequestException ex,
            HttpServletRequest request) {

        Map<String, Object> body = baseBody(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(MlDependencyFailureException.class)
    public ResponseEntity<Map<String, Object>> handleMlDependencyFailure(
            MlDependencyFailureException ex,
            HttpServletRequest request) {

        Map<String, Object> body = baseBody(HttpStatus.FAILED_DEPENDENCY, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.FAILED_DEPENDENCY).body(body);
    }

    @ExceptionHandler(MlServiceUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleMlServiceUnavailable(
            MlServiceUnavailableException ex,
            HttpServletRequest request) {

        Map<String, Object> body = baseBody(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    @ExceptionHandler(MlRateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMlRateLimitExceeded(
            MlRateLimitExceededException ex,
            HttpServletRequest request) {

        Map<String, Object> body = baseBody(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(body);
    }

    @ExceptionHandler(NlpAnalysisNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNlpAnalysisNotFound(
            NlpAnalysisNotFoundException ex,
            HttpServletRequest request) {

        Map<String, Object> body = baseBody(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(NlpClientException.class)
    public ResponseEntity<Map<String, Object>> handleNlpClientException(
            NlpClientException ex,
            HttpServletRequest request) {

        Map<String, Object> body = baseBody(HttpStatus.BAD_GATEWAY, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> fieldErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage, (first, ignored) -> first, LinkedHashMap::new));

        Map<String, Object> body = baseBody(HttpStatus.BAD_REQUEST, "Validation failed", request.getRequestURI());
        body.put("fieldErrors", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        Map<String, Object> body = baseBody(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        Map<String, Object> body = baseBody(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        String message = "Invalid data: request violates database constraints";
        if (ex.getMostSpecificCause() != null && ex.getMostSpecificCause().getMessage() != null) {
            message = ex.getMostSpecificCause().getMessage();
        }

        Map<String, Object> body = baseBody(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        Map<String, Object> body = baseBody(HttpStatus.BAD_REQUEST, "Malformed or invalid request payload", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(
            RuntimeException ex,
            HttpServletRequest request) {

        Map<String, Object> body = baseBody(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private Map<String, Object> baseBody(HttpStatus status, String message, String path) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", path);
        return body;
    }
}
