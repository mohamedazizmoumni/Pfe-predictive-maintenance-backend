package com.pfe.predictive.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * A malformed request parameter (e.g. a non-numeric value sent for a
 * Long-typed @RequestParam, like assignedTechnicianId) must surface as 400
 * Bad Request, not fall through to the catch-all RuntimeException handler
 * and be misreported as a 500 server fault.
 */
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Mock
    private HttpServletRequest request;

    @Mock
    private MethodArgumentTypeMismatchException typeMismatch;

    @Test
    void methodArgumentTypeMismatchReturnsBadRequestNotServerError() {
        when(request.getRequestURI()).thenReturn("/api/v1/maintenance");
        when(typeMismatch.getName()).thenReturn("assignedTechnicianId");
        when(typeMismatch.getValue()).thenReturn("technicien1");
        doReturn(Long.class).when(typeMismatch).getRequiredType();

        ResponseEntity<Map<String, Object>> response = handler.handleMethodArgumentTypeMismatch(typeMismatch, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        String message = (String) response.getBody().get("message");
        assertTrue(message.contains("assignedTechnicianId"));
        assertTrue(message.contains("Long"));
        assertTrue(message.contains("technicien1"));
    }

    @Test
    void unrelatedRuntimeExceptionsStillReturnServerError() {
        when(request.getRequestURI()).thenReturn("/api/v1/machines");

        ResponseEntity<Map<String, Object>> response =
                handler.handleRuntimeException(new RuntimeException("boom"), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }
}
