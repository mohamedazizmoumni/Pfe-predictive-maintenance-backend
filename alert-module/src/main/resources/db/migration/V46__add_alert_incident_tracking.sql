-- V46: Add incident-tracking columns to alerts
-- Purpose: Replace time-based alert cooldown with a state-based incident model —
-- only one active alert per (machine_id, issue_type) at a time.

ALTER TABLE alerts
    ADD COLUMN IF NOT EXISTS issue_type VARCHAR(100),
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS resolved_date TIMESTAMP,
    ADD COLUMN IF NOT EXISTS last_email_sent_at TIMESTAMP;

ALTER TABLE alerts DROP CONSTRAINT IF EXISTS alerts_status_check;
ALTER TABLE alerts ADD CONSTRAINT alerts_status_check
    CHECK (status IN ('NEW', 'ACKNOWLEDGED', 'IN_PROGRESS', 'ESCALATED', 'RESOLVED', 'CLOSED'));

-- Enforce "one active incident per machine+issueType" at the DB level too,
-- not just in application logic.
CREATE UNIQUE INDEX IF NOT EXISTS idx_alerts_active_incident
    ON alerts(machine_id, issue_type) WHERE is_active = TRUE;

CREATE INDEX IF NOT EXISTS idx_alerts_issue_type ON alerts(issue_type);

COMMENT ON COLUMN alerts.issue_type IS 'Dedup key alongside machine_id identifying which kind of problem this incident is';
COMMENT ON COLUMN alerts.is_active IS 'True while the underlying machine condition is still ongoing (the incident is open)';
COMMENT ON COLUMN alerts.resolved_date IS 'When the system auto-resolved this incident because the machine recovered';
COMMENT ON COLUMN alerts.last_email_sent_at IS 'When an email notification was last sent for this alert';
