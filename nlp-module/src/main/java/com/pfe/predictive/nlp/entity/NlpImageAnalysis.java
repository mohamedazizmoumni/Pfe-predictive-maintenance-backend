package com.pfe.predictive.nlp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "nlp_image_analyses", indexes = {
    @Index(name = "idx_nlp_image_analyses_machine_id", columnList = "machine_id"),
    @Index(name = "idx_nlp_image_analyses_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NlpImageAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "machine_id", nullable = false)
    private Long machineId;

    @Column(name = "attachment_id", nullable = false)
    private Long attachmentId;

    /** PENDING while the background vision call is running, then COMPLETE or FAILED. */
    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "risk_level", length = 50)
    private String riskLevel;

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "nlp_image_analysis_keywords", joinColumns = @JoinColumn(name = "analysis_id"))
    @OrderColumn(name = "keyword_order")
    @Column(name = "keyword", length = 255)
    private List<String> keywords = new ArrayList<>();

    @Column(name = "model_version", length = 50)
    private String modelVersion;

    @Column(name = "model_backend", length = 50)
    private String modelBackend;

    @Column(name = "processing_time_ms", precision = 10, scale = 2)
    private BigDecimal processingTimeMs;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
