-- Maintenance Module Migration
-- V5: Create maintenance table and indexes
-- Purpose: Maintenance task scheduling and execution tracking

CREATE TABLE IF NOT EXISTS maintenance (
    id BIGSERIAL PRIMARY KEY,
    machine_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL CHECK (type IN ('PREVENTIVE', 'CORRECTIVE', 'EMERGENCY')),
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM' CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    description TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'SCHEDULED' CHECK (status IN ('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'APPROVED', 'CANCELLED')),
    scheduled_date TIMESTAMP NOT NULL,
    start_date TIMESTAMP,
    completed_date TIMESTAMP,
    approved_date TIMESTAMP,
    estimated_duration INTEGER, -- minutes
    assigned_technician_id BIGINT,
    approved_by VARCHAR(100),
    notes TEXT,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    last_modified_by VARCHAR(100),
    version INTEGER DEFAULT 0,
    
    CONSTRAINT fk_maintenance_machine FOREIGN KEY (machine_id) REFERENCES machines(id) ON DELETE CASCADE,
    CONSTRAINT fk_maintenance_technician FOREIGN KEY (assigned_technician_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_dates CHECK (start_date IS NULL OR scheduled_date <= start_date),
    CONSTRAINT chk_completion CHECK (completed_date IS NULL OR start_date <= completed_date),
    CONSTRAINT chk_approval CHECK (approved_date IS NULL OR completed_date <= approved_date)
);

-- Indexes for common queries
CREATE INDEX idx_maintenance_machine_id ON maintenance(machine_id);
CREATE INDEX idx_maintenance_status ON maintenance(status);
CREATE INDEX idx_maintenance_type ON maintenance(type);
CREATE INDEX idx_maintenance_scheduled_date ON maintenance(scheduled_date);
CREATE INDEX idx_maintenance_technician_id ON maintenance(assigned_technician_id);
CREATE INDEX idx_maintenance_created_date ON maintenance(created_date DESC);
CREATE INDEX idx_maintenance_machine_status ON maintenance(machine_id, status);
CREATE INDEX idx_maintenance_scheduled_status ON maintenance(scheduled_date, status);

-- ==================== COMMENT ====================
COMMENT ON TABLE maintenance IS 'Stores maintenance tasks and schedules for machines';
COMMENT ON COLUMN maintenance.type IS 'Type of maintenance: PREVENTIVE (scheduled), CORRECTIVE (unplanned), EMERGENCY (urgent)';
COMMENT ON COLUMN maintenance.status IS 'Maintenance lifecycle: SCHEDULED, IN_PROGRESS, COMPLETED, APPROVED, CANCELLED';
COMMENT ON COLUMN maintenance.priority IS 'Task priority affecting scheduling: LOW, MEDIUM, HIGH, CRITICAL';
COMMENT ON COLUMN maintenance.estimated_duration IS 'Estimated time in minutes to complete maintenance';
COMMENT ON COLUMN maintenance.assigned_technician_id IS 'User ID of assigned technician';
