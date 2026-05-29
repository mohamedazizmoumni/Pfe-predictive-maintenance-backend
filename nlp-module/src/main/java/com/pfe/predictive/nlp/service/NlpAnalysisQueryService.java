package com.pfe.predictive.nlp.service;

import com.pfe.predictive.nlp.dto.NlpAnalysisDTO;
import com.pfe.predictive.nlp.mapper.NlpAnalysisMapper;
import com.pfe.predictive.nlp.repository.NlpAnalysisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NlpAnalysisQueryService {

    private final NlpAnalysisRepository nlpAnalysisRepository;
    private final NlpAnalysisMapper nlpAnalysisMapper;

    @Transactional(readOnly = true)
    public Page<NlpAnalysisDTO> findHistory(Pageable pageable) {
        return nlpAnalysisMapper.toAnalysisPage(nlpAnalysisRepository.findByOrderByCreatedAtDesc(pageable));
    }

    @Transactional(readOnly = true)
    public Page<NlpAnalysisDTO> findByMachineId(Long machineId, Pageable pageable) {
        return nlpAnalysisMapper.toAnalysisPage(nlpAnalysisRepository.findByMachineIdOrderByCreatedAtDesc(machineId, pageable));
    }

    @Transactional(readOnly = true)
    public Page<NlpAnalysisDTO> findRecommendations(Pageable pageable) {
        return nlpAnalysisMapper.toAnalysisPage(nlpAnalysisRepository.findByRecommendationIsNotNullOrderByCreatedAtDesc(pageable));
    }
}
