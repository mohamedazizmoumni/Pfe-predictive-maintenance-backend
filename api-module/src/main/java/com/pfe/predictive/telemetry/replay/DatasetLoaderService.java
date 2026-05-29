package com.pfe.predictive.telemetry.replay;

import com.pfe.predictive.telemetry.replay.model.DatasetTelemetryRow;
import com.pfe.predictive.telemetry.replay.model.DatasetTrajectory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
public class DatasetLoaderService {

    private final TelemetryReplayProperties properties;
    private final ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
    private final AtomicReference<List<DatasetTrajectory>> cachedTrajectories = new AtomicReference<>();

    public DatasetLoaderService(TelemetryReplayProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void warmUp() {
        if (properties.isEnabled()) {
            loadTrajectories();
        }
    }

    public List<DatasetTrajectory> loadTrajectories() {
        List<DatasetTrajectory> cached = cachedTrajectories.get();
        if (cached != null) {
            return cached;
        }

        synchronized (cachedTrajectories) {
            cached = cachedTrajectories.get();
            if (cached != null) {
                return cached;
            }

            List<DatasetTrajectory> trajectories = new ArrayList<>();
            List<String> resources = properties.getDatasetResources();

            if (resources == null || resources.isEmpty()) {
                log.warn("No NASA C-MAPSS dataset resources configured. Telemetry replay will stay idle until telemetry.replay.dataset-resources is set.");
                cachedTrajectories.set(List.of());
                return List.of();
            }

            for (String location : resources) {
                if (location == null || location.isBlank()) {
                    continue;
                }

                try {
                    Resource[] resolvedResources = resourcePatternResolver.getResources(location.trim());
                    if (resolvedResources.length == 0) {
                        log.warn("Dataset resource pattern '{}' did not resolve to any file", location);
                    }

                    for (Resource resource : resolvedResources) {
                        if (!resource.exists() || !resource.isReadable()) {
                            log.warn("Skipping unreadable dataset resource: {}", resource.getDescription());
                            continue;
                        }
                        trajectories.addAll(parseResource(resource));
                    }
                } catch (IOException ex) {
                    throw new IllegalStateException("Failed to resolve dataset resource pattern: " + location, ex);
                }
            }

            if (trajectories.isEmpty()) {
                log.warn("No trajectories loaded from configured NASA C-MAPSS resources");
            } else {
                log.info("Loaded {} dataset trajectories for replay", trajectories.size());
            }

            List<DatasetTrajectory> immutableTrajectories = List.copyOf(trajectories);
            cachedTrajectories.set(immutableTrajectories);
            return immutableTrajectories;
        }
    }

    private List<DatasetTrajectory> parseResource(Resource resource) throws IOException {
        String sourceName = resource.getFilename() != null ? resource.getFilename() : resource.getDescription();
        Map<Integer, List<ParsedRow>> rowsByUnit = new LinkedHashMap<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }

                String[] tokens = trimmed.split("[\\s,]+");
                if (tokens.length < 26) {
                    log.warn("Skipping malformed NASA C-MAPSS row in {}: expected at least 26 columns, got {}", sourceName, tokens.length);
                    continue;
                }

                ParsedRow parsedRow = new ParsedRow(
                        parseInt(tokens[0], sourceName, trimmed),
                        parseInt(tokens[1], sourceName, trimmed),
                        parseDouble(tokens[2], sourceName, trimmed),
                        parseDouble(tokens[3], sourceName, trimmed),
                        parseDouble(tokens[4], sourceName, trimmed),
                        parseSensors(tokens, sourceName, trimmed)
                );

                rowsByUnit.computeIfAbsent(parsedRow.unitId(), ignored -> new ArrayList<>()).add(parsedRow);
            }
        }

        List<DatasetTrajectory> trajectories = new ArrayList<>();
        for (Map.Entry<Integer, List<ParsedRow>> entry : rowsByUnit.entrySet()) {
            Integer unitId = entry.getKey();
            List<ParsedRow> parsedRows = entry.getValue();
            parsedRows.sort((left, right) -> Integer.compare(left.cycle(), right.cycle()));

            String trajectoryId = sourceName + ":unit-" + unitId;
            List<DatasetTelemetryRow> telemetryRows = new ArrayList<>(parsedRows.size());
            for (ParsedRow parsedRow : parsedRows) {
                telemetryRows.add(new DatasetTelemetryRow(
                        trajectoryId,
                        sourceName,
                        unitId,
                        parsedRow.cycle(),
                        parsedRow.setting1(),
                        parsedRow.setting2(),
                        parsedRow.setting3(),
                        parsedRow.sensors()));
            }

            trajectories.add(new DatasetTrajectory(trajectoryId, sourceName, unitId, telemetryRows));
        }

        return trajectories;
    }

    private int parseInt(String token, String sourceName, String rawLine) {
        try {
            return Integer.parseInt(token);
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Failed to parse integer in dataset resource " + sourceName + ": " + rawLine, ex);
        }
    }

    private double parseDouble(String token, String sourceName, String rawLine) {
        try {
            return Double.parseDouble(token);
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Failed to parse numeric value in dataset resource " + sourceName + ": " + rawLine, ex);
        }
    }

    private double[] parseSensors(String[] tokens, String sourceName, String rawLine) {
        if (tokens.length < 26) {
            throw new IllegalStateException("Malformed NASA C-MAPSS row in " + sourceName + ": " + rawLine);
        }

        double[] sensors = new double[21];
        for (int sensorIndex = 0; sensorIndex < sensors.length; sensorIndex++) {
            sensors[sensorIndex] = parseDouble(tokens[5 + sensorIndex], sourceName, rawLine);
        }
        return sensors;
    }

    private record ParsedRow(
            int unitId,
            int cycle,
            double setting1,
            double setting2,
            double setting3,
            double[] sensors) {
    }
}
