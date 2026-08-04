package com.pfe.predictive.nlp.repository;

import com.pfe.predictive.nlp.entity.NlpImageAnalysis;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NlpImageAnalysisRepository extends JpaRepository<NlpImageAnalysis, Long> {

    Page<NlpImageAnalysis> findByMachineIdOrderByCreatedAtDesc(Long machineId, Pageable pageable);
}
