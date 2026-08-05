package com.pfe.predictive.nlp.vision;

import com.pfe.predictive.nlp.client.NlpImageAnalysisClientResponse;
import com.pfe.predictive.nlp.dto.NlpImageAnalysisDTO;
import com.pfe.predictive.nlp.entity.NlpImageAnalysis;
import com.pfe.predictive.nlp.repository.NlpImageAnalysisRepository;
import com.pfe.predictive.nlp.websocket.NlpAnalysisPublisher;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A separate bean (not methods on NlpImageAnalysisAsyncRunner) for the same
 * proxy reason documented there for {@code @Async}: NlpImageAnalysisAsyncRunner
 * calls these as a same-class self-invocation, which bypasses the Spring
 * proxy and would silently drop {@code @Transactional} too.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NlpImageAnalysisCompletionService {

    private final NlpImageAnalysisRepository nlpImageAnalysisRepository;
    private final NlpAnalysisPublisher nlpAnalysisPublisher;

    @Transactional
    public void completeAnalysis(Long analysisId, NlpImageAnalysisClientResponse analysis) {
        NlpImageAnalysis entity = nlpImageAnalysisRepository.findById(analysisId).orElse(null);
        if (entity == null) {
            log.warn("Image analysis {} vanished before completion could be recorded", analysisId);
            return;
        }

        entity.setStatus("COMPLETE");
        entity.setDescription(analysis.getDescription());
        entity.setRiskLevel(analysis.getRiskLevel());
        entity.setKeywords(analysis.getKeywords() != null ? new ArrayList<>(analysis.getKeywords()) : new ArrayList<>());
        entity.setModelVersion(analysis.getModelVersion());
        entity.setModelBackend(analysis.getModelBackend());
        entity.setProcessingTimeMs(analysis.getProcessingTimeMs());

        NlpImageAnalysis saved = nlpImageAnalysisRepository.save(entity);
        NlpImageAnalysisDTO response = NlpImageAnalysisMapper.toDto(saved, analysis.getMessage());
        nlpAnalysisPublisher.publishImageAnalysis(response);
    }

    @Transactional
    public void failAnalysis(Long analysisId, String errorMessage) {
        NlpImageAnalysis entity = nlpImageAnalysisRepository.findById(analysisId).orElse(null);
        if (entity == null) {
            return;
        }
        entity.setStatus("FAILED");
        entity.setErrorMessage(errorMessage);
        NlpImageAnalysis saved = nlpImageAnalysisRepository.save(entity);

        NlpImageAnalysisDTO response = NlpImageAnalysisMapper.toDto(saved,
                "I couldn't finish analyzing that photo, but it's saved to the record — you can try again.");
        nlpAnalysisPublisher.publishImageAnalysis(response);
    }
}
