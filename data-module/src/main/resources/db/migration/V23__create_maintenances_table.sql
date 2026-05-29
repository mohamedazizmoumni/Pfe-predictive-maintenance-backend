-- Maintenance module entity uses the plural table name maintenances

CREATE TABLE IF NOT EXISTS maintenances (
    id BIGSERIAL PRIMARY KEY,
    machine_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL CHECK (type IN ('PREVENTIVE', 'CORRECTIVE', 'PREDICTIVE')),
    description VARCHAR(1000),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PLANNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    scheduled_date TIMESTAMP NOT NULL,
    start_date TIMESTAMP,
    completed_date TIMESTAMP,
    estimated_hours INTEGER,
    actual_hours INTEGER,
    assigned_to VARCHAR(100),
    notes VARCHAR(1000),
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP,
    version BIGINT,

    CONSTRAINT fk_maintenances_machine FOREIGN KEY (machine_id)
        REFERENCES machines(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_maintenances_machine_id ON maintenances(machine_id);
CREATE INDEX IF NOT EXISTS idx_maintenances_status ON maintenances(status);
CREATE INDEX IF NOT EXISTS idx_maintenances_type ON maintenances(type);
CREATE INDEX IF NOT EXISTS idx_maintenances_scheduled_date ON maintenances(scheduled_date);
CREATE INDEX IF NOT EXISTS idx_maintenances_created_date ON maintenances(created_date DESC);
