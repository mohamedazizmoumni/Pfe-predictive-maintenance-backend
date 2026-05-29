-- V17: Create Notifications Table
-- Purpose: Store HIGH and CRITICAL risk notifications from ML predictions

CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    machine_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT,
    risk_level VARCHAR(20) NOT NULL CHECK (risk_level IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW')),
    prediction_record_id BIGINT,
    target_roles VARCHAR(100),
    is_read BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_notifications_machine FOREIGN KEY (machine_id) 
        REFERENCES machines(id) ON DELETE CASCADE,
    CONSTRAINT fk_notifications_prediction FOREIGN KEY (prediction_record_id) 
        REFERENCES predictions(id) ON DELETE SET NULL
);

-- Indexes for common queries
CREATE INDEX idx_notification_machine_time ON notifications(machine_id, created_at DESC);
CREATE INDEX idx_notification_is_read ON notifications(is_read);
CREATE INDEX idx_notification_created_at ON notifications(created_at DESC);

-- COMMENT ON TABLE
COMMENT ON TABLE notifications IS 'Stores HIGH and CRITICAL risk notifications triggered by ML predictions';
COMMENT ON COLUMN notifications.machine_id IS 'Reference to the machine that triggered the notification';
COMMENT ON COLUMN notifications.risk_level IS 'Risk level that triggered notification: CRITICAL, HIGH';
COMMENT ON COLUMN notifications.target_roles IS 'Comma-separated roles that should see this notification (e.g., TECHNICIAN,MANAGER)';
COMMENT ON COLUMN notifications.is_read IS 'Whether the notification has been acknowledged by a user';
