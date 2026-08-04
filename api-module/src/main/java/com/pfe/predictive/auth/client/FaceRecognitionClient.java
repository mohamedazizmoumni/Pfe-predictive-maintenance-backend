package com.pfe.predictive.auth.client;

import com.pfe.predictive.auth.config.FaceLoginProperties;
import com.pfe.predictive.auth.dto.FaceRecognitionResult;
import com.pfe.predictive.auth.exception.FaceLoginUnauthorizedException;
import com.pfe.predictive.auth.exception.FaceNotDetectedException;
import com.pfe.predictive.ml.exception.MlBadRequestException;
import com.pfe.predictive.ml.exception.MlDependencyFailureException;
import com.pfe.predictive.ml.exception.MlServiceUnavailableException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Locale;

@Component
public class FaceRecognitionClient {

    // Use the secured RestTemplate bean that injects X-Internal-Key header
    private final RestTemplate restTemplate;
    private final FaceLoginProperties properties;

    public FaceRecognitionClient(@Qualifier("faceRestTemplate") RestTemplate restTemplate, FaceLoginProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    public FaceRecognitionResult recognize(MultipartFile image) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            ByteArrayResource imageResource = new ByteArrayResource(image.getBytes()) {
                @Override
                public String getFilename() {
                    String original = image.getOriginalFilename();
                    return (original == null || original.isBlank()) ? "face-image.jpg" : original;
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add(properties.getImageFieldName(), imageResource);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    buildRecognizeUrl(),
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new MlDependencyFailureException("Face recognition service returned an invalid response");
            }

            return extractRecognitionResult(response.getBody());
        } catch (HttpStatusCodeException ex) {
            throw mapHttpStatusException(ex);
        } catch (ResourceAccessException ex) {
            throw new MlServiceUnavailableException("Face recognition service is unavailable", ex);
        } catch (IOException ex) {
            throw new MlDependencyFailureException("Failed to read uploaded image file", ex);
        }
    }

    public void enrollFace(Long userId, MultipartFile file) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    String original = file.getOriginalFilename();
                    return (original == null || original.isBlank()) ? "face-image.jpg" : original;
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add(properties.getImageFieldName(), fileResource);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    buildEnrollUrl(userId),
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new MlDependencyFailureException("Face enrollment service returned an invalid response");
            }
        } catch (HttpStatusCodeException ex) {
            throw mapHttpStatusException(ex);
        } catch (ResourceAccessException ex) {
            throw new MlServiceUnavailableException("Face recognition service is unavailable", ex);
        } catch (IOException ex) {
            throw new MlDependencyFailureException("Failed to read uploaded image file", ex);
        }
    }

    private RuntimeException mapHttpStatusException(HttpStatusCodeException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String responseBody = ex.getResponseBodyAsString();

        if (status == HttpStatus.UNAUTHORIZED || status == HttpStatus.NOT_FOUND) {
            return new FaceLoginUnauthorizedException("No matching face/user was found");
        }

        if (status == HttpStatus.BAD_REQUEST || status == HttpStatus.UNPROCESSABLE_ENTITY) {
            if (containsFaceNotDetectedMessage(responseBody)) {
                return new FaceNotDetectedException("Face not detected in the uploaded image");
            }
            return new MlBadRequestException("Face recognition request was rejected by Python service");
        }

        if (status.is5xxServerError()) {
            return new MlServiceUnavailableException("Face recognition service is unavailable", ex);
        }

        return new MlDependencyFailureException("Unexpected face recognition error: HTTP " + status.value(), ex);
    }

    private boolean containsFaceNotDetectedMessage(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return false;
        }

        String normalized = responseBody.toLowerCase(Locale.ROOT);
        return normalized.contains("face") &&
                (normalized.contains("not detected") || normalized.contains("no face") || normalized.contains("cannot detect"));
    }

    private FaceRecognitionResult extractRecognitionResult(Map<?, ?> body) {
        Long userId = extractUserId(body);
        Double confidence = extractConfidence(body);
        return new FaceRecognitionResult(userId, confidence);
    }

    private Long extractUserId(Map<?, ?> body) {
        Object value = firstNonNull(
                body.get("user_id"),
                body.get("userId"),
                body.get("matched_user_id"),
                body.get("match_id"),
                body.get("id"),
                body.get("user")
        );

        if (value instanceof Map<?, ?> nestedUser) {
            value = firstNonNull(
                    nestedUser.get("user_id"),
                    nestedUser.get("userId"),
                    nestedUser.get("id")
            );
        }

        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.longValue();
        }

        if (value instanceof String text) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        return null;
    }

    private Double extractConfidence(Map<?, ?> body) {
        Object value = firstNonNull(
                body.get("confidence"),
                body.get("score"),
                body.get("similarity"),
                body.get("distance")
        );

        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.doubleValue();
        }

        if (value instanceof String text) {
            try {
                return Double.parseDouble(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        return null;
    }

    private Object firstNonNull(Object... values) {
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String buildRecognizeUrl() {
        String base = properties.getBaseUrl() == null ? "" : properties.getBaseUrl().trim();
        String path = properties.getRecognizePath() == null ? "" : properties.getRecognizePath().trim();

        if (base.endsWith("/") && path.startsWith("/")) {
            return base.substring(0, base.length() - 1) + path;
        }

        if (!base.endsWith("/") && !path.startsWith("/")) {
            return base + "/" + path;
        }

        return base + path;
    }

    private String buildEnrollUrl(Long userId) {
        String base = properties.getBaseUrl() == null ? "" : properties.getBaseUrl().trim();
        String path = properties.getEnrollPath() == null ? "" : properties.getEnrollPath().trim();

        String url;
        if (base.endsWith("/") && path.startsWith("/")) {
            url = base.substring(0, base.length() - 1) + path;
        } else if (!base.endsWith("/") && !path.startsWith("/")) {
            url = base + "/" + path;
        } else {
            url = base + path;
        }

        return url + "?user_id=" + userId;
    }

    /**
     * Delete all stored embeddings for a user from the ML service.
     * Called by the admin face-reset endpoint. Errors are best-effort;
     * the caller should not fail if this throws.
     */
    public void deleteEnrollment(Long userId) {
        try {
            String base = properties.getBaseUrl() == null ? "" : properties.getBaseUrl().trim();
            // DELETE /face/enroll?user_id={id}  — matches the Python router path
            String path = properties.getEnrollPath() == null ? "/face/enroll" : properties.getEnrollPath().trim();

            String url;
            if (base.endsWith("/") && path.startsWith("/")) {
                url = base.substring(0, base.length() - 1) + path;
            } else if (!base.endsWith("/") && !path.startsWith("/")) {
                url = base + "/" + path;
            } else {
                url = base + path;
            }
            url = url + "?user_id=" + userId;

            restTemplate.exchange(url, HttpMethod.DELETE, HttpEntity.EMPTY, String.class);
        } catch (Exception ex) {
            // Best-effort — log and continue
            throw new MlDependencyFailureException("Could not delete ML embedding for user " + userId + ": " + ex.getMessage(), ex);
        }
    }
}
