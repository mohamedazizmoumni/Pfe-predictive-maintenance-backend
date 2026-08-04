package com.pfe.predictive.nlp.vision;

import com.pfe.predictive.attachment.service.AttachmentService;
import com.pfe.predictive.core.entity.Attachment;
import com.pfe.predictive.nlp.dto.NlpImageAnalysisDTO;
import com.pfe.predictive.nlp.entity.NlpImageAnalysis;
import com.pfe.predictive.nlp.exception.NlpClientException;
import com.pfe.predictive.nlp.repository.NlpImageAnalysisRepository;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

/**
 * Lives in api-module (not alongside NlpAnalysisService in nlp-module)
 * because it needs AttachmentService, which is an api-module class —
 * nlp-module has no dependency on api-module, so the orchestration has to
 * sit on this side of that boundary.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NlpImageAnalysisService {

    static final String PENDING_MESSAGE =
            "Analyzing your photo — this runs on a local model and can take a few minutes. "
            + "Feel free to keep working; I'll update this the moment it's ready.";

    private final AttachmentService attachmentService;
    private final NlpImageAnalysisRepository nlpImageAnalysisRepository;
    private final NlpImageAnalysisAsyncRunner asyncRunner;

    /**
     * Returns as soon as the photo is safely stored and a PENDING record
     * exists — the actual vision-model call runs several minutes on
     * CPU-only hardware (see NlpWebClientConfig's nlpVisionWebClient
     * timeout) so it must never sit on the request thread. The caller gets
     * the real result later over the same WebSocket topics
     * NlpAnalysisPublisher already pushes text-analysis results to.
     */
    @Transactional
    public NlpImageAnalysisDTO submitForAnalysis(MultipartFile image, Long machineId, String context, String uploadedBy) {
        if (machineId == null) {
            throw new IllegalArgumentException("machineId is required");
        }
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("A non-empty image file is required");
        }

        log.info("Accepting equipment photo for analysis — machine {}", machineId);

        // Save first: the photo must survive even if the vision call
        // subsequently fails or times out — uploaded photos are never
        // analyze-and-discard.
        Attachment attachment;
        try {
            attachment = attachmentService.upload(image, "Machine", machineId, uploadedBy);
        } catch (IOException ex) {
            throw new NlpClientException("Failed to store equipment photo: " + ex.getMessage(), ex);
        }

        NlpImageAnalysis pending = NlpImageAnalysis.builder()
                .machineId(machineId)
                .attachmentId(attachment.getId())
                .status("PENDING")
                .build();
        NlpImageAnalysis saved = nlpImageAnalysisRepository.save(pending);

        // Kick off the background analysis only once this row is actually
        // committed and visible — otherwise the async runner could look it
        // up before the INSERT is durable.
        scheduleAfterCommit(() -> asyncRunner.runAnalysis(
                saved.getId(), attachment.getStoredFileName(), attachment.getFileName(),
                attachment.getContentType(), machineId, context));

        return NlpImageAnalysisMapper.toDto(saved, PENDING_MESSAGE);
    }

    private void scheduleAfterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
            return;
        }
        task.run();
    }
}
