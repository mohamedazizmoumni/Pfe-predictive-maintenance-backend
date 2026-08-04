package com.pfe.predictive.nlp.vision;

import com.pfe.predictive.attachment.service.RepairEvidenceStorage;
import com.pfe.predictive.nlp.client.NlpImageAnalysisClient;
import com.pfe.predictive.nlp.client.NlpImageAnalysisClientResponse;
import com.pfe.predictive.nlp.dto.NlpImageAnalysisDTO;
import com.pfe.predictive.nlp.entity.NlpImageAnalysis;
import com.pfe.predictive.nlp.repository.NlpImageAnalysisRepository;
import com.pfe.predictive.nlp.websocket.NlpAnalysisPublisher;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A separate bean (not a method on NlpImageAnalysisService) specifically so
 * {@code @Async} actually takes effect — Spring's proxy-based @Async only
 * intercepts calls that arrive through the bean's proxy, and a same-class
 * self-invocation (this.someAsyncMethod()) bypasses that proxy entirely and
 * would run synchronously, silently defeating the whole point of moving this
 * off the request thread.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NlpImageAnalysisAsyncRunner {

    private final RepairEvidenceStorage storage;
    private final NlpImageAnalysisClient nlpImageAnalysisClient;
    private final NlpImageAnalysisRepository nlpImageAnalysisRepository;
    private final NlpAnalysisPublisher nlpAnalysisPublisher;

    @Async("taskExecutor")
    public void runAnalysis(Long analysisId, String storedFileName, String originalFilename,
                             String contentType, Long machineId, String context) {
        try {
            byte[] imageBytes = readStoredFile(storedFileName);
            NlpImageAnalysisClientResponse analysis =
                    nlpImageAnalysisClient.analyze(imageBytes, originalFilename, contentType, machineId, context);
            completeAnalysis(analysisId, analysis);
        } catch (Exception ex) {
            log.error("Equipment photo analysis {} failed for machine {}: {}",
                    analysisId, machineId, ex.getMessage(), ex);
            failAnalysis(analysisId, ex.getMessage());
        }
    }

    private byte[] readStoredFile(String storedFileName) throws IOException {
        Resource resource = storage.loadAsResource(storedFileName);
        if (resource == null || !resource.exists()) {
            throw new IOException("Stored photo not found: " + storedFileName);
        }
        return Files.readAllBytes(resource.getFile().toPath());
    }

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
