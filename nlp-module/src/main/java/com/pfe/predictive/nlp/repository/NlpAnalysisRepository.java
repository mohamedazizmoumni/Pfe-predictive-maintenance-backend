package com.pfe.predictive.nlp.repository;

import com.pfe.predictive.nlp.entity.NlpAnalysis;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface NlpAnalysisRepository extends JpaRepository<NlpAnalysis, Long> {

    Page<NlpAnalysis> findByOrderByCreatedAtDesc(Pageable pageable);

    Page<NlpAnalysis> findByMachineIdOrderByCreatedAtDesc(Long machineId, Pageable pageable);

        @Query("""
                select n
                from NlpAnalysis n
                where n.recommendation is not null
                    and trim(n.recommendation) <> ''
                order by n.createdAt desc
                """)
        Page<NlpAnalysis> findByRecommendationIsNotNullOrderByCreatedAtDesc(Pageable pageable);

    List<NlpAnalysis> findTop20ByMachineIdOrderByCreatedAtDesc(Long machineId);
}
