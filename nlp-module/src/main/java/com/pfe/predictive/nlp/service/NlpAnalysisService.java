package com.pfe.predictive.nlp.service;

import com.pfe.predictive.nlp.client.NlpAnalysisClient;
import com.pfe.predictive.nlp.dto.NlpRequestDTO;
import com.pfe.predictive.nlp.dto.NlpResponseDTO;
import com.pfe.predictive.nlp.entity.NlpAnalysis;
import com.pfe.predictive.nlp.mapper.NlpAnalysisMapper;
import com.pfe.predictive.nlp.repository.NlpAnalysisRepository;
import com.pfe.predictive.nlp.websocket.NlpAnalysisPublisher;
import jakarta.transaction.Transactional;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@Slf4j
@RequiredArgsConstructor
public class NlpAnalysisService {

    private final NlpAnalysisClient nlpAnalysisClient;
    private final NlpAnalysisRepository nlpAnalysisRepository;
    private final NlpAnalysisMapper nlpAnalysisMapper;
    private final NlpAnalysisPublisher nlpAnalysisPublisher;

    @Transactional
    public NlpResponseDTO analyzeAndStore(NlpRequestDTO request) {
        validateRequest(request);

        log.info("Analyzing technician report for machine {}", request.getMachineId());

        NlpResponseDTO analysisResponse = nlpAnalysisClient.analyze(request);
        NlpAnalysis entity = nlpAnalysisMapper.toEntity(request, analysisResponse);
        NlpAnalysis saved = nlpAnalysisRepository.save(Objects.requireNonNull(entity, "NLP analysis entity cannot be null"));
        NlpResponseDTO response = nlpAnalysisMapper.toResponse(saved);

        publishAfterCommit(response);

        return response;
    }

    private void publishAfterCommit(NlpResponseDTO response) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    nlpAnalysisPublisher.publishAnalysis(response);
                }
            });
            return;
        }

        nlpAnalysisPublisher.publishAnalysis(response);
    }

    private void validateRequest(NlpRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("NLP analysis request is required");
        }
        if (request.getMachineId() == null) {
            throw new IllegalArgumentException("machineId is required");
        }
        if (!StringUtils.hasText(request.getRawText())) {
            throw new IllegalArgumentException("rawText is required");
        }
    }
}
