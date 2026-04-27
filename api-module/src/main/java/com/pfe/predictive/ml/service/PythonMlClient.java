package com.pfe.predictive.ml.service;

import com.pfe.predictive.ml.config.MlServiceProperties;
import com.pfe.predictive.ml.dto.ModelInfoResponse;
import com.pfe.predictive.ml.dto.PredictionRequest;
import com.pfe.predictive.ml.dto.PredictionResponse;
import com.pfe.predictive.ml.exception.MlBadRequestException;
import com.pfe.predictive.ml.exception.MlDependencyFailureException;
import com.pfe.predictive.ml.exception.MlServiceUnavailableException;
import com.pfe.predictive.ml.exception.MlTransientFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.function.Supplier;

@Service
public class PythonMlClient {

    private static final Logger log = LoggerFactory.getLogger(PythonMlClient.class);

    private final RestClient restClient;
    private final MlServiceProperties properties;

    public PythonMlClient(@Qualifier("pythonMlRestClient") RestClient restClient,
                          MlServiceProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public Map<String, Object> health(String correlationId) {
        return executeWithRetry("health", () -> restClient.get()
                .uri("/health")
                .header(properties.getCorrelationHeader(), correlationId)
                .retrieve()
                .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                    throw new MlTransientFailureException("Python ML health endpoint returned 5xx: " + response.getStatusCode().value());
                })
                .body(Map.class));
    }

    public ModelInfoResponse modelInfo(String correlationId) {
        ModelInfoResponse modelInfoResponse = executeWithRetry("model-info", () -> restClient.get()
                .uri("/model-info")
                .header(properties.getCorrelationHeader(), correlationId)
                .retrieve()
                .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                    throw new MlTransientFailureException("Python ML model-info endpoint returned 5xx: " + response.getStatusCode().value());
                })
                .body(ModelInfoResponse.class));

        if (modelInfoResponse == null) {
            throw new MlDependencyFailureException("Python ML model-info returned empty response");
        }
        return modelInfoResponse;
    }

    public PredictionResponse predict(PredictionRequest request, String correlationId) {
        PredictionResponse response = executeWithRetry("predict", () -> restClient.post()
                .uri("/predict")
                .header(properties.getCorrelationHeader(), correlationId)
                .body(request)
                .retrieve()
                .onStatus(status -> status.value() == 422, (req, res) -> {
                    throw new MlBadRequestException("Python ML rejected request schema or feature shape");
                })
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new MlBadRequestException("Prediction request rejected by Python ML service with status " + res.getStatusCode().value());
                })
                .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                    throw new MlTransientFailureException("Python ML predict endpoint returned 5xx: " + res.getStatusCode().value());
                })
                .body(PredictionResponse.class));

        if (response == null || response.getPrediction() == null) {
            throw new MlDependencyFailureException("Python ML service returned an invalid prediction response");
        }
        return response;
    }

    private <T> T executeWithRetry(String operation, Supplier<T> operationSupplier) {
        int maxAttempts = Math.max(1, properties.getRetry().getMaxAttempts());
        long backoffMs = Math.max(0, properties.getRetry().getBackoff().toMillis());

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return operationSupplier.get();
            } catch (MlBadRequestException ex) {
                throw ex;
            } catch (MlTransientFailureException ex) {
                if (attempt == maxAttempts) {
                    throw new MlDependencyFailureException("Python ML service failed after retries for operation: " + operation, ex);
                }
                log.warn("Transient ML failure on operation={} attempt={}/{}; retrying", operation, attempt, maxAttempts);
                sleepBackoff(backoffMs);
            } catch (ResourceAccessException ex) {
                if (attempt == maxAttempts) {
                    throw new MlServiceUnavailableException("Python ML service is unavailable for operation: " + operation, ex);
                }
                log.warn("Network ML failure on operation={} attempt={}/{}; retrying", operation, attempt, maxAttempts);
                sleepBackoff(backoffMs);
            } catch (RestClientResponseException ex) {
                if (ex.getStatusCode().is5xxServerError()) {
                    if (attempt == maxAttempts) {
                        throw new MlDependencyFailureException("Python ML service returned 5xx after retries for operation: " + operation, ex);
                    }
                    log.warn("5xx ML response on operation={} attempt={}/{}; retrying", operation, attempt, maxAttempts);
                    sleepBackoff(backoffMs);
                    continue;
                }
                throw new MlBadRequestException("Python ML request failed with status " + ex.getStatusCode().value());
            } catch (RestClientException ex) {
                if (isTimeoutOrConnect(ex)) {
                    if (attempt == maxAttempts) {
                        throw new MlServiceUnavailableException("Python ML service timed out or is unreachable for operation: " + operation, ex);
                    }
                    log.warn("ML timeout/connect failure on operation={} attempt={}/{}; retrying", operation, attempt, maxAttempts);
                    sleepBackoff(backoffMs);
                    continue;
                }
                throw new MlDependencyFailureException("Unexpected ML client error during operation: " + operation, ex);
            }
        }

        throw new MlDependencyFailureException("Retry loop exited unexpectedly for operation: " + operation);
    }

    private void sleepBackoff(long backoffMs) {
        if (backoffMs <= 0) {
            return;
        }
        try {
            Thread.sleep(backoffMs);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    static boolean isTimeoutOrConnect(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor != null) {
            if (cursor instanceof SocketTimeoutException || cursor instanceof ConnectException) {
                return true;
            }
            cursor = cursor.getCause();
        }
        return false;
    }
}
