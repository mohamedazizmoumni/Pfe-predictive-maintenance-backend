package com.pfe.predictive.nlp.service;

import com.pfe.predictive.nlp.client.NlpAnalysisClient;
import com.pfe.predictive.nlp.dto.NlpRequestDTO;
import com.pfe.predictive.nlp.dto.NlpResponseDTO;
import com.pfe.predictive.nlp.entity.NlpAnalysis;
import com.pfe.predictive.nlp.mapper.NlpAnalysisMapper;
import com.pfe.predictive.nlp.repository.NlpAnalysisRepository;
import com.pfe.predictive.nlp.websocket.NlpAnalysisPublisher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers request validation and the publish-after-commit vs. publish-now
 * branching in NlpAnalysisService: inside an active Spring transaction the
 * realtime broadcast must wait for afterCommit (never fire on a rolled-back
 * save), but outside one it must fire immediately.
 */
@ExtendWith(MockitoExtension.class)
class NlpAnalysisServiceTest {

    @Mock
    private NlpAnalysisClient nlpAnalysisClient;

    @Mock
    private NlpAnalysisRepository nlpAnalysisRepository;

    @Mock
    private NlpAnalysisPublisher nlpAnalysisPublisher;

    private NlpAnalysisService service;

    @BeforeEach
    void setUp() {
        service = new NlpAnalysisService(nlpAnalysisClient, nlpAnalysisRepository, new NlpAnalysisMapper(), nlpAnalysisPublisher);
    }

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private NlpRequestDTO validRequest() {
        return NlpRequestDTO.builder().machineId(1L).rawText("Bearing making grinding noise").build();
    }

    private NlpResponseDTO clientResponse() {
        return NlpResponseDTO.builder()
                .failureType("BEARING_FAILURE")
                .riskLevel("HIGH")
                .confidence(0.87)
                .message("Sounds like a bearing issue.")
                .intent("DIAGNOSTIC")
                .isQuestion(false)
                .cleanedText("bearing making grinding noise")
                .modelVersion("v2.3")
                .processingTimeMs(42.0)
                .build();
    }

    // ------------------------------------------------------------------
    // validation guards
    // ------------------------------------------------------------------

    @Test
    void rejectsNullRequest() {
        assertThrows(IllegalArgumentException.class, () -> service.analyzeAndStore(null));
    }

    @Test
    void rejectsMissingMachineId() {
        NlpRequestDTO request = NlpRequestDTO.builder().rawText("noise").build();
        assertThrows(IllegalArgumentException.class, () -> service.analyzeAndStore(request));
    }

    @Test
    void rejectsBlankRawText() {
        NlpRequestDTO request = NlpRequestDTO.builder().machineId(1L).rawText("   ").build();
        assertThrows(IllegalArgumentException.class, () -> service.analyzeAndStore(request));
    }

    // ------------------------------------------------------------------
    // successful flow
    // ------------------------------------------------------------------

    @Test
    void reattachesConversationalFieldsNotPersistedByTheMapper() {
        when(nlpAnalysisClient.analyze(any())).thenReturn(clientResponse());
        when(nlpAnalysisRepository.save(any(NlpAnalysis.class))).thenAnswer(inv -> {
            NlpAnalysis entity = inv.getArgument(0);
            entity.setId(1L);
            return entity;
        });

        NlpResponseDTO response = service.analyzeAndStore(validRequest());

        assertEquals("Sounds like a bearing issue.", response.getMessage());
        assertEquals("DIAGNOSTIC", response.getIntent());
        assertEquals(false, response.getIsQuestion());
        assertEquals("bearing making grinding noise", response.getCleanedText());
        assertEquals("v2.3", response.getModelVersion());
        assertEquals(42.0, response.getProcessingTimeMs());
        // Persisted fields still come through the saved entity.
        assertEquals("BEARING_FAILURE", response.getFailureType());
    }

    @Test
    void publishesImmediatelyWhenNoTransactionIsActive() {
        when(nlpAnalysisClient.analyze(any())).thenReturn(clientResponse());
        when(nlpAnalysisRepository.save(any(NlpAnalysis.class))).thenAnswer(inv -> {
            NlpAnalysis entity = inv.getArgument(0);
            entity.setId(1L);
            return entity;
        });

        service.analyzeAndStore(validRequest());

        verify(nlpAnalysisPublisher, times(1)).publishAnalysis(any());
    }

    @Test
    void deferPublishUntilAfterCommitWhenTransactionSynchronizationIsActive() {
        when(nlpAnalysisClient.analyze(any())).thenReturn(clientResponse());
        when(nlpAnalysisRepository.save(any(NlpAnalysis.class))).thenAnswer(inv -> {
            NlpAnalysis entity = inv.getArgument(0);
            entity.setId(1L);
            return entity;
        });

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.analyzeAndStore(validRequest());

            // Not published yet -- only registered to publish after commit.
            verify(nlpAnalysisPublisher, never()).publishAnalysis(any());

            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(org.springframework.transaction.support.TransactionSynchronization::afterCommit);

            verify(nlpAnalysisPublisher, times(1)).publishAnalysis(any());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
