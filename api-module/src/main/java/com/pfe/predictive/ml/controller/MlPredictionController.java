package com.pfe.predictive.ml.controller;

import com.pfe.predictive.config.CorrelationIdFilter;
import com.pfe.predictive.ml.dto.ModelInfoResponse;
import com.pfe.predictive.ml.dto.PredictionApiRequest;
import com.pfe.predictive.ml.dto.PredictionResponse;
import com.pfe.predictive.ml.service.MlMetadataService;
import com.pfe.predictive.ml.service.MlPredictionService;
import com.pfe.predictive.ml.service.PythonMlClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ml")
public class MlPredictionController {

    private final MlPredictionService mlPredictionService;
    private final MlMetadataService mlMetadataService;
    private final PythonMlClient pythonMlClient;

    public MlPredictionController(MlPredictionService mlPredictionService,
                                  MlMetadataService mlMetadataService,
                                  PythonMlClient pythonMlClient) {
        this.mlPredictionService = mlPredictionService;
        this.mlMetadataService = mlMetadataService;
        this.pythonMlClient = pythonMlClient;
    }

    @GetMapping("/health")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> health(HttpServletRequest request) {
        String correlationId = resolveCorrelationId(request);
        return ResponseEntity.ok(pythonMlClient.health(correlationId));
    }

    @GetMapping("/model-info")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ModelInfoResponse> modelInfo(HttpServletRequest request) {
        String correlationId = resolveCorrelationId(request);
        return ResponseEntity.ok(mlMetadataService.getModelInfo(correlationId));
    }

    @PostMapping("/predict")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PredictionResponse> predict(@Valid @RequestBody PredictionApiRequest request,
                                                      HttpServletRequest servletRequest) {
        String correlationId = resolveCorrelationId(servletRequest);
        String requestId = UUID.randomUUID().toString();
        Long machineId = resolveMachineId(servletRequest);
        PredictionResponse response = mlPredictionService.predict(machineId, request.getFeatures(), correlationId, requestId);
        return ResponseEntity.ok(response);
    }

    private Long resolveMachineId(HttpServletRequest request) {
        String machineIdHeader = request.getHeader("X-Machine-ID");
        if (machineIdHeader == null || machineIdHeader.isBlank()) {
            return -1L;
        }

        try {
            return Long.parseLong(machineIdHeader);
        } catch (NumberFormatException ex) {
            return -1L;
        }
    }

    private String resolveCorrelationId(HttpServletRequest request) {
        Object value = request.getAttribute(CorrelationIdFilter.CORRELATION_ID_ATTR);
        if (value instanceof String text && !text.isBlank()) {
            return text;
        }

        String header = request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        if (header != null && !header.isBlank()) {
            return header;
        }
        return UUID.randomUUID().toString();
    }
}
