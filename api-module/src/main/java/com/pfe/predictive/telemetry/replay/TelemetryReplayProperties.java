package com.pfe.predictive.telemetry.replay;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "telemetry.replay")
public class TelemetryReplayProperties {

    private boolean enabled = true;
    private long tickIntervalMs = 2000L;
    private double defaultSpeed = 1.0;
    private double cycleDurationHours = 1.0;
    private double degradingHealthThreshold = 70.0;
    private double criticalHealthThreshold = 30.0;
    private boolean autoResetOnFailure = true;
    private boolean pauseAtEndOfTrajectory = true;
    private List<String> datasetResources = new ArrayList<>();
}
