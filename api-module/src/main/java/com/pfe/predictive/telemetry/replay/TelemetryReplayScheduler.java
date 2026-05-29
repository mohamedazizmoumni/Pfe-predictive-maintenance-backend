package com.pfe.predictive.telemetry.replay;

import com.pfe.predictive.websocket.service.MachineStreamingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class TelemetryReplayScheduler {

    private final TelemetryReplayProperties properties;
    private final MachineStreamingService machineStreamingService;

    @Scheduled(fixedRateString = "${telemetry.replay.tick-interval-ms:2000}")
    public void replayTick() {
        if (!properties.isEnabled()) {
            return;
        }

        machineStreamingService.streamReplayTick();
    }

    public void triggerOnce() {
        if (!properties.isEnabled()) {
            log.info("Telemetry replay is disabled");
            return;
        }
        machineStreamingService.streamReplayTick();
    }
}
