-- V33: Create tasks table
-- Purpose: Store task management data for maintenance and operations

CREATE TABLE IF NOT EXISTS tasks (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    priority VARCHAR(50) NOT NULL DEFAULT 'MEDIUM',
    machine_id BIGINT,
    maintenance_id BIGINT,
    assigned_to VARCHAR(100),
    due_date TIMESTAMP,
    completed_date TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    
    CONSTRAINT fk_tasks_machine FOREIGN KEY (machine_id) REFERENCES machines(id) ON DELETE SET NULL,
    CONSTRAINT fk_tasks_maintenance FOREIGN KEY (maintenance_id) REFERENCES maintenance(id) ON DELETE SET NULL
);

-- Create indexes for common queries
CREATE INDEX IF NOT EXISTS idx_tasks_status ON tasks(status);
CREATE INDEX IF NOT EXISTS idx_tasks_priority ON tasks(priority);
CREATE INDEX IF NOT EXISTS idx_tasks_machine_id ON tasks(machine_id);
CREATE INDEX IF NOT EXISTS idx_tasks_maintenance_id ON tasks(maintenance_id);
CREATE INDEX IF NOT EXISTS idx_tasks_assigned_to ON tasks(assigned_to);
CREATE INDEX IF NOT EXISTS idx_tasks_due_date ON tasks(due_date);
CREATE INDEX IF NOT EXISTS idx_tasks_created_at ON tasks(created_at);

-- Add comments for documentation
COMMENT ON TABLE tasks IS 'Stores task management data for maintenance and operations';
COMMENT ON COLUMN tasks.status IS 'Task status: PENDING, IN_PROGRESS, COMPLETED, CANCELLED';
COMMENT ON COLUMN tasks.priority IS 'Task priority: LOW, MEDIUM, HIGH, CRITICAL';
