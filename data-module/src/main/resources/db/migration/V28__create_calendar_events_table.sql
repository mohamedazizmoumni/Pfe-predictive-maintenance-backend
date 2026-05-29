-- Create calendar_events table for technician scheduling
CREATE TABLE IF NOT EXISTS calendar_events (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    event_type VARCHAR(100), -- TASK, MEETING, MAINTENANCE, INSPECTION
    assigned_to VARCHAR(100) NOT NULL,
    status VARCHAR(50), -- SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED
    priority VARCHAR(50), -- LOW, MEDIUM, HIGH, URGENT
    location VARCHAR(255),
    machine_id BIGINT,
    task_id BIGINT,
    notes VARCHAR(1000),
    is_all_day BOOLEAN DEFAULT FALSE,
    is_recurring BOOLEAN DEFAULT FALSE,
    recurrence_pattern VARCHAR(50), -- DAILY, WEEKLY, MONTHLY, YEARLY
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

-- Create indexes for common queries
CREATE INDEX idx_calendar_assigned_to ON calendar_events(assigned_to);
CREATE INDEX idx_calendar_start_time ON calendar_events(start_time);
CREATE INDEX idx_calendar_machine_id ON calendar_events(machine_id);
CREATE INDEX idx_calendar_task_id ON calendar_events(task_id);
CREATE INDEX idx_calendar_event_type ON calendar_events(event_type);
CREATE INDEX idx_calendar_status ON calendar_events(status);
