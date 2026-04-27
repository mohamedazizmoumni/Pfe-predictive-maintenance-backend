package com.pfe.predictive.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "face-login.service")
public class FaceLoginProperties {

    private String baseUrl = "http://localhost:5001";
    private String recognizePath = "/recognize";
    private String enrollPath = "/enroll";
    private String imageFieldName = "image";
    private final Timeout timeout = new Timeout();

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getRecognizePath() {
        return recognizePath;
    }

    public void setRecognizePath(String recognizePath) {
        this.recognizePath = recognizePath;
    }

    public String getEnrollPath() {
        return enrollPath;
    }

    public void setEnrollPath(String enrollPath) {
        this.enrollPath = enrollPath;
    }

    public String getImageFieldName() {
        return imageFieldName;
    }

    public void setImageFieldName(String imageFieldName) {
        this.imageFieldName = imageFieldName;
    }

    public Timeout getTimeout() {
        return timeout;
    }

    public static class Timeout {
        private Duration connect = Duration.ofSeconds(2);
        private Duration read = Duration.ofSeconds(8);

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
}
