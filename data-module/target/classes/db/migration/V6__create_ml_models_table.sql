-- Prediction Module Migration (Part 1)
-- V6: Create ML Model table
-- Purpose: Store machine learning models and their performance metrics

CREATE TABLE IF NOT EXISTS ml_models (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    model_type VARCHAR(100) NOT NULL, -- NEURAL_NETWORK, RANDOM_FOREST, SVM, etc.
    model_version VARCHAR(50) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED')),
    training_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deactivated_date TIMESTAMP,
    accuracy NUMERIC(5,2), -- 0-100
    precision NUMERIC(5,2), -- 0-100
    recall NUMERIC(5,2), -- 0-100
    f1_score NUMERIC(5,2), -- 0-100
    model_path VARCHAR(500),
    parameters TEXT, -- JSON with hyperparameters
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    last_modified_by VARCHAR(100),
    version INTEGER DEFAULT 0,
    
    CONSTRAINT chk_accuracy_range CHECK (accuracy >= 0 AND accuracy <= 100),
    CONSTRAINT chk_model_metrics_range CHECK (precision >= 0 AND precision <= 100 AND recall >= 0 AND recall <= 100)
);

CREATE INDEX idx_ml_models_status ON ml_models(status);
CREATE INDEX idx_ml_models_type ON ml_models(model_type);
CREATE INDEX idx_ml_models_training_date ON ml_models(training_date DESC);
CREATE INDEX idx_ml_models_created_date ON ml_models(created_date DESC);

-- ==================== COMMENT ====================
COMMENT ON TABLE ml_models IS 'Stores machine learning models used for predictive maintenance';
COMMENT ON COLUMN ml_models.status IS 'Model operational status: ACTIVE (in use), INACTIVE (paused), ARCHIVED (discontinued)';
COMMENT ON COLUMN ml_models.accuracy IS 'Model accuracy percentage from training/validation (0-100)';
COMMENT ON COLUMN ml_models.model_path IS 'File system or cloud path where model artifacts are stored';
