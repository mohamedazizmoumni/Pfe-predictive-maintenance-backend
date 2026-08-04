-- V48: Add predicted_failure_type to alerts
-- Purpose: Surface the ML model's predicted failure mode so the incident
-- email can describe the specific detected issue, not just a generic
-- severity-tier sentence.

ALTER TABLE alerts
    ADD COLUMN IF NOT EXISTS predicted_failure_type VARCHAR(50);

COMMENT ON COLUMN alerts.predicted_failure_type IS 'ML-predicted failure mode snapshot (e.g. BEARING_FAILURE, GENERAL_DEGRADATION)';
