-- ============================================================================
-- V11__create_alerts_table.sql
-- ============================================================================
-- Create alerts table for the alert module
-- Tracks system alerts for machine issues, anomalies, and warnings
-- Status Flow: NEW → ACKNOWLEDGED → ESCALATED → CLOSED
-- ============================================================================

CREATE TABLE IF NOT EXISTS alerts (
    id BIGSERIAL PRIMARY KEY,
    machine_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    message VARCHAR(2000),
    severity VARCHAR(20) NOT NULL CHECK (severity IN ('INFO', 'WARNING', 'CRITICAL')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('NEW', 'ACKNOWLEDGED', 'ESCALATED', 'CLOSED')),
    category VARCHAR(50),
    source_reference VARCHAR(100),
    viewed BOOLEAN DEFAULT FALSE NOT NULL,
    assigned_to VARCHAR(100),
    created_by VARCHAR(100),
    acknowledged_by VARCHAR(100),
    acknowledged_date TIMESTAMP,
    escalated_by VARCHAR(100),
    escalated_date TIMESTAMP,
    closed_by VARCHAR(100),
    closed_date TIMESTAMP,
    resolution_notes VARCHAR(1000),
    recommendations VARCHAR(1000),
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,

    CONSTRAINT fk_alert_machine FOREIGN KEY (machine_id) REFERENCES machines(id) ON DELETE CASCADE
);

-- ============================================================================
-- INDEXES FOR PERFORMANCE
-- ============================================================================
CREATE INDEX IF NOT EXISTS idx_alerts_machine_id ON alerts(machine_id);
CREATE INDEX IF NOT EXISTS idx_alerts_status ON alerts(status);
CREATE INDEX IF NOT EXISTS idx_alerts_assigned_to ON alerts(assigned_to);
CREATE INDEX IF NOT EXISTS idx_alerts_created_date ON alerts(created_date DESC);
CREATE INDEX IF NOT EXISTS idx_alerts_severity ON alerts(severity);
CREATE INDEX IF NOT EXISTS idx_alerts_status_severity ON alerts(status, severity);
CREATE INDEX IF NOT EXISTS idx_alerts_assigned_status ON alerts(assigned_to, status);

-- ============================================================================
-- AUDIT LOG TABLE (Optional: for tracking alert state changes)
-- ============================================================================
CREATE TABLE IF NOT EXISTS alert_audit_log (
    id BIGSERIAL PRIMARY KEY,
    alert_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    old_status VARCHAR(20),
    new_status VARCHAR(20),
    changed_by VARCHAR(100) NOT NULL,
    changed_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes VARCHAR(1000),

    CONSTRAINT fk_audit_alert FOREIGN KEY (alert_id) REFERENCES alerts(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_audit_alert_id ON alert_audit_log(alert_id);
CREATE INDEX IF NOT EXISTS idx_audit_changed_date ON alert_audit_log(changed_date DESC);

-- ============================================================================
-- COMMENTS
-- ============================================================================
COMMENT ON TABLE alerts IS 'System alerts for machine issues, anomalies, and warnings. Supports lifecycle: NEW → ACKNOWLEDGED → ESCALATED → CLOSED';
COMMENT ON COLUMN alerts.severity IS 'Alert priority: INFO (low), WARNING (medium), CRITICAL (high)';
COMMENT ON COLUMN alerts.status IS 'Alert lifecycle status';
COMMENT ON COLUMN alerts.assigned_to IS 'Technician or user assigned to handle this alert';
COMMENT ON COLUMN alerts.viewed IS 'Has the assigned user viewed this alert?';
