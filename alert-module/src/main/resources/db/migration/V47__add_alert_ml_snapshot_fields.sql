-- V47: Add ML snapshot fields to alerts + HIGH severity tier
-- Purpose: Make the Alert row the single source of truth for everything an
-- incident's notification email displays (severity, health, ML analysis).

ALTER TABLE alerts
    ADD COLUMN IF NOT EXISTS health_score DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS predicted_rul DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS anomaly_probability DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS risk_level VARCHAR(20),
    ADD COLUMN IF NOT EXISTS failure_probability DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS anomaly_type VARCHAR(50);

ALTER TABLE alerts DROP CONSTRAINT IF EXISTS alerts_severity_check;
ALTER TABLE alerts ADD CONSTRAINT alerts_severity_check
    CHECK (severity IN ('INFO', 'WARNING', 'HIGH', 'CRITICAL'));

COMMENT ON COLUMN alerts.health_score IS 'Machine health % at the time this incident was opened/last updated';
COMMENT ON COLUMN alerts.predicted_rul IS 'ML-predicted remaining useful life (cycles) snapshot';
COMMENT ON COLUMN alerts.anomaly_probability IS 'ML anomaly probability snapshot';
COMMENT ON COLUMN alerts.risk_level IS 'ML risk level snapshot (LOW/MEDIUM/HIGH/CRITICAL) — for reference only, not the same as alerts.severity';
COMMENT ON COLUMN alerts.failure_probability IS 'ML failure probability snapshot';
COMMENT ON COLUMN alerts.anomaly_type IS 'ML anomaly type classification snapshot';
