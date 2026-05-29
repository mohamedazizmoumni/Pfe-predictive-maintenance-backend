CREATE TABLE IF NOT EXISTS nlp_analyses (
    id BIGSERIAL PRIMARY KEY,
    machine_id BIGINT NOT NULL,
    raw_text TEXT NOT NULL,
    failure_type VARCHAR(200),
    risk_level VARCHAR(50),
    recommendation TEXT,
    root_cause TEXT,
    confidence NUMERIC(5,4),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_nlp_analyses_machine_id ON nlp_analyses (machine_id);
CREATE INDEX IF NOT EXISTS idx_nlp_analyses_created_at ON nlp_analyses (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_nlp_analyses_risk_level ON nlp_analyses (risk_level);

CREATE TABLE IF NOT EXISTS nlp_analysis_keywords (
    analysis_id BIGINT NOT NULL,
    keyword_order INTEGER NOT NULL,
    keyword VARCHAR(255) NOT NULL,
    PRIMARY KEY (analysis_id, keyword_order),
    CONSTRAINT fk_nlp_analysis_keywords_analysis
        FOREIGN KEY (analysis_id) REFERENCES nlp_analyses (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_nlp_analysis_keywords_analysis_id ON nlp_analysis_keywords (analysis_id);
