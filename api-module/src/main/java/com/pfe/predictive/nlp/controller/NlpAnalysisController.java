package com.pfe.predictive.nlp.controller;

import com.pfe.predictive.nlp.dto.NlpAnalysisDTO;
import com.pfe.predictive.nlp.dto.NlpRequestDTO;
import com.pfe.predictive.nlp.dto.NlpResponseDTO;
import com.pfe.predictive.nlp.service.NlpAnalysisQueryService;
import com.pfe.predictive.nlp.service.NlpAnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/nlp", "/api/v1/nlp"})
@RequiredArgsConstructor
public class NlpAnalysisController {

    private static final String NLP_ROLE_SCOPE =
        "hasAnyAuthority('ROLE_TECHNICIAN','ROLE_MANAGER','ROLE_DATA_SCIENTIST','ROLE_ADMIN','ROLE_SUPER_ADMIN')";

    private final NlpAnalysisService nlpAnalysisService;
    private final NlpAnalysisQueryService nlpAnalysisQueryService;

    @PostMapping("/analyze")
    @PreAuthorize(NLP_ROLE_SCOPE)
    public ResponseEntity<NlpResponseDTO> analyze(@Valid @RequestBody NlpRequestDTO request) {
        NlpResponseDTO response = nlpAnalysisService.analyzeAndStore(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/history")
    @PreAuthorize(NLP_ROLE_SCOPE)
    public ResponseEntity<Page<NlpAnalysisDTO>> history(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(nlpAnalysisQueryService.findHistory(pageable));
    }

    @GetMapping("/machine/{machineId}")
    @PreAuthorize(NLP_ROLE_SCOPE)
    public ResponseEntity<Page<NlpAnalysisDTO>> machineHistory(
            @PathVariable Long machineId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(nlpAnalysisQueryService.findByMachineId(machineId, pageable));
    }

    @GetMapping("/recommendations")
    @PreAuthorize(NLP_ROLE_SCOPE)
    public ResponseEntity<Page<NlpAnalysisDTO>> recommendations(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(nlpAnalysisQueryService.findRecommendations(pageable));
    }
}