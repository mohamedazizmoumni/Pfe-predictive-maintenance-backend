package com.pfe.predictive.nlp.vision;

import com.pfe.predictive.attachment.service.RepairEvidenceStorage;
import com.pfe.predictive.nlp.client.NlpImageAnalysisClient;
import com.pfe.predictive.nlp.client.NlpImageAnalysisClientResponse;
import java.io.IOException;
import java.nio.file.Files;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * A separate bean (not a method on NlpImageAnalysisService) specifically so
 * {@code @Async} actually takes effect — Spring's proxy-based @Async only
 * intercepts calls that arrive through the bean's proxy, and a same-class
 * self-invocation (this.someAsyncMethod()) bypasses that proxy entirely and
 * would run synchronously, silently defeating the whole point of moving this
 * off the request thread.
 *
 * completeAnalysis/failAnalysis live on NlpImageAnalysisCompletionService,
 * a further separate bean, for the identical reason: calling them as a
 * same-class self-invocation from here would bypass the proxy and silently
 * drop their {@code @Transactional} too.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NlpImageAnalysisAsyncRunner {

    private final RepairEvidenceStorage storage;
    private final NlpImageAnalysisClient nlpImageAnalysisClient;
    private final NlpImageAnalysisCompletionService completionService;

    @Async("taskExecutor")
    public void runAnalysis(Long analysisId, String storedFileName, String originalFilename,
                             String contentType, Long machineId, String context) {
        try {
            byte[] imageBytes = readStoredFile(storedFileName);
            NlpImageAnalysisClientResponse analysis =
                    nlpImageAnalysisClient.analyze(imageBytes, originalFilename, contentType, machineId, context);
            completionService.completeAnalysis(analysisId, analysis);
        } catch (Exception ex) {
            log.error("Equipment photo analysis {} failed for machine {}: {}",
                    analysisId, machineId, ex.getMessage(), ex);
            completionService.failAnalysis(analysisId, ex.getMessage());
        }
    }

    private byte[] readStoredFile(String storedFileName) throws IOException {
        Resource resource = storage.loadAsResource(storedFileName);
        if (resource == null || !resource.exists()) {
            throw new IOException("Stored photo not found: " + storedFileName);
        }
        return Files.readAllBytes(resource.getFile().toPath());
    }
}
