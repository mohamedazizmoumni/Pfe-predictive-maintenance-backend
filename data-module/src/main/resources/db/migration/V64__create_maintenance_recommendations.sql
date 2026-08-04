-- Persisted, actionable AI maintenance recommendations — supports
-- approve/reject workflow and history, on top of the existing stateless
-- /recommendations/generate preview endpoint.
CREATE TABLE IF NOT EXISTS maintenance_recommendations (
    id                        BIGSERIAL PRIMARY KEY,
    machine_id                BIGINT        NOT NULL REFERENCES machines (id) ON DELETE CASCADE,
    urgency_level             VARCHAR(20)   NOT NULL,
    recommended_action        VARCHAR(20)   NOT NULL,
    justification             VARCHAR(2000),
    estimated_cost            NUMERIC(19,2),
    estimated_savings         NUMERIC(19,2),
    failure_probability       DOUBLE PRECISION,
    days_until_failure        INT,
    status                    VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    generated_at              TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    decided_by                VARCHAR(100),
    decided_at                TIMESTAMP,
    decision_note             VARCHAR(1000),
    resulting_maintenance_id  BIGINT
);

CREATE INDEX IF NOT EXISTS idx_maintenance_recommendations_status ON maintenance_recommendations (status, generated_at);
CREATE INDEX IF NOT EXISTS idx_maintenance_recommendations_machine ON maintenance_recommendations (machine_id);
