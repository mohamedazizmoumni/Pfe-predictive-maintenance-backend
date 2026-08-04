package com.pfe.predictive.nlp.vision;

import com.pfe.predictive.nlp.dto.NlpImageAnalysisDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping({"/api/nlp", "/api/v1/nlp"})
@RequiredArgsConstructor
public class NlpImageAnalysisController {

    // Same scope as NlpAnalysisController.NLP_ROLE_SCOPE — this is a
    // sibling action on the same NLP Dashboard chat page, not a separate
    // permission domain, and there's no shared PermissionConstants entry
    // for NLP to reference instead.
    private static final String NLP_ROLE_SCOPE =
        "hasAnyAuthority('ROLE_TECHNICIAN','ROLE_MANAGER','ROLE_DATA_SCIENTIST','ROLE_ADMIN','ROLE_SUPER_ADMIN')";

    private final NlpImageAnalysisService nlpImageAnalysisService;

    @PostMapping(value = "/analyze-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(NLP_ROLE_SCOPE)
    public ResponseEntity<NlpImageAnalysisDTO> analyzeImage(
            @RequestPart("image") MultipartFile image,
            @RequestParam Long machineId,
            @RequestParam(required = false) String context,
            Authentication authentication) {
        // Accepted, not created-and-complete: the photo is saved and queued
        // for background analysis, which can take several minutes on
        // CPU-only hardware (see NlpImageAnalysisAsyncRunner) — the caller
        // gets the finished result later over the NLP WebSocket topics.
        NlpImageAnalysisDTO response = nlpImageAnalysisService.submitForAnalysis(
                image, machineId, context, authentication.getName());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
