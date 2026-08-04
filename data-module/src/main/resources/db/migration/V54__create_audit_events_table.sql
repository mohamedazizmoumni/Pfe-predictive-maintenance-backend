-- Platform-wide governance audit log. Distinct from predictive_action_audit
-- (V22), which is machine-scoped (NOT NULL FK to machines) and unsuitable for
-- non-machine events such as a role assignment or a user deletion.
CREATE TABLE IF NOT EXISTS audit_events (
    id BIGSERIAL PRIMARY KEY,
    actor VARCHAR(100) NOT NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100),
    entity_id BIGINT,
    details VARCHAR(2000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_events_actor ON audit_events(actor);
CREATE INDEX IF NOT EXISTS idx_audit_events_entity ON audit_events(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_events_created_at ON audit_events(created_at DESC);
