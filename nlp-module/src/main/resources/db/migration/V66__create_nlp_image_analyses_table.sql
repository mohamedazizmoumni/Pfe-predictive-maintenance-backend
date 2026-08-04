-- Vision analysis is a genuinely slow, CPU-bound local-model call (multiple
-- minutes on modest hardware — see NlpImageAnalysisService) so a row is
-- inserted as PENDING immediately on upload and updated in place once the
-- background analysis finishes; description/risk_level/keywords are only
-- populated at that point, hence nullable.
CREATE TABLE IF NOT EXISTS nlp_image_analyses (
    id BIGSERIAL PRIMARY KEY,
    machine_id BIGINT NOT NULL,
    attachment_id BIGINT NOT NULL REFERENCES attachments (id),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    description TEXT,
    risk_level VARCHAR(50),
    model_version VARCHAR(50),
    model_backend VARCHAR(50),
    processing_time_ms NUMERIC(10,2),
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_nlp_image_analyses_machine_id ON nlp_image_analyses (machine_id);
CREATE INDEX IF NOT EXISTS idx_nlp_image_analyses_created_at ON nlp_image_analyses (created_at DESC);

CREATE TABLE IF NOT EXISTS nlp_image_analysis_keywords (
    analysis_id BIGINT NOT NULL,
    keyword_order INTEGER NOT NULL,
    keyword VARCHAR(255) NOT NULL,
    PRIMARY KEY (analysis_id, keyword_order),
    CONSTRAINT fk_nlp_image_analysis_keywords_analysis
        FOREIGN KEY (analysis_id) REFERENCES nlp_image_analyses (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_nlp_image_analysis_keywords_analysis_id ON nlp_image_analysis_keywords (analysis_id);
