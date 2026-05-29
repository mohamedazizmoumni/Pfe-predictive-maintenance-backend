-- Predictive action audit table for tracking machine actions and ML-driven recommendations

CREATE TABLE IF NOT EXISTS predictive_action_audit (
    id BIGSERIAL PRIMARY KEY,
    machine_id BIGINT NOT NULL,
    action_type VARCHAR(100) NOT NULL,
    details VARCHAR(2000) NOT NULL,
    actor VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_predictive_action_audit_machine FOREIGN KEY (machine_id)
        REFERENCES machines(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_predictive_action_audit_machine_id ON predictive_action_audit(machine_id);
CREATE INDEX IF NOT EXISTS idx_predictive_action_audit_created_at ON predictive_action_audit(created_at DESC);
