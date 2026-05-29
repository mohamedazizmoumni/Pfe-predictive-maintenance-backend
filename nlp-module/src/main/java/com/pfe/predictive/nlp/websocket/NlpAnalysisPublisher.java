package com.pfe.predictive.nlp.websocket;

import com.pfe.predictive.nlp.dto.NlpResponseDTO;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NlpAnalysisPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    @Async("taskExecutor")
    public void publishAnalysis(NlpResponseDTO response) {
        if (response == null) {
            return;
        }

        try {
            Map<String, Object> alertPayload = buildPayload(response, "NLP_ALERT");
            Map<String, Object> insightPayload = buildPayload(response, "MACHINE_INSIGHT");

            messagingTemplate.convertAndSend("/topic/nlp-alerts", alertPayload);
            messagingTemplate.convertAndSend("/topic/machine-insights", insightPayload);

            log.debug("Published NLP analysis {} to realtime topics", response.getId());
        } catch (Exception ex) {
            log.error("Failed to publish NLP analysis {}: {}", response.getId(), ex.getMessage(), ex);
        }
    }

    private Map<String, Object> buildPayload(NlpResponseDTO response, String eventType) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", eventType);
        payload.put("analysisId", response.getId());
        payload.put("machineId", response.getMachineId());
        payload.put("failureType", response.getFailureType());
        payload.put("riskLevel", response.getRiskLevel());
        payload.put("keywords", response.getKeywords());
        payload.put("recommendation", response.getRecommendation());
        payload.put("rootCause", response.getRootCause());
        payload.put("confidence", response.getConfidence());
        payload.put("createdAt", response.getCreatedAt() == null ? LocalDateTime.now() : response.getCreatedAt());
        return payload;
    }
}
