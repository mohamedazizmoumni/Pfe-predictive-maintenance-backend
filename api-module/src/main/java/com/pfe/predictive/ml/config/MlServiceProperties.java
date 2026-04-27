package com.pfe.predictive.ml.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "ml.service")
public class MlServiceProperties {

    private String baseUrl = "http://127.0.0.1:8000";
    private int expectedFeatureCount = 89;
    private String correlationHeader = "X-Correlation-ID";
    private final Timeout timeout = new Timeout();
    private final Retry retry = new Retry();
    private final Auth auth = new Auth();

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getExpectedFeatureCount() {
        return expectedFeatureCount;
    }

    public void setExpectedFeatureCount(int expectedFeatureCount) {
        this.expectedFeatureCount = expectedFeatureCount;
    }

    public String getCorrelationHeader() {
        return correlationHeader;
    }

    public void setCorrelationHeader(String correlationHeader) {
        this.correlationHeader = correlationHeader;
    }

    public Timeout getTimeout() {
        return timeout;
    }

    public Retry getRetry() {
        return retry;
    }

    public Auth getAuth() {
        return auth;
    }

    public static class Timeout {
        private Duration connect = Duration.ofSeconds(2);
        private Duration read = Duration.ofSeconds(5);

        public Duration getConnect() {
            return connect;
        }

        public void setConnect(Duration connect) {
            this.connect = connect;
        }

        public Duration getRead() {
            return read;
        }

        public void setRead(Duration read) {
            this.read = read;
        }
    }

    public static class Retry {
        private int maxAttempts = 3;
        private Duration backoff = Duration.ofMillis(200);

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public Duration getBackoff() {
            return backoff;
        }

        public void setBackoff(Duration backoff) {
            this.backoff = backoff;
        }
    }

    public static class Auth {
        private String headerName = "";
        private String headerValue = "";

        public String getHeaderName() {
            return headerName;
        }

        public void setHeaderName(String headerName) {
            this.headerName = headerName;
        }

        public String getHeaderValue() {
            return headerValue;
        }

        public void setHeaderValue(String headerValue) {
            this.headerValue = headerValue;
        }
    }
}
