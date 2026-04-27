-- Maintenance cost module schema

CREATE TABLE IF NOT EXISTS maintenance_cost_machines (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    location VARCHAR(255) NOT NULL,
    status VARCHAR(40) NOT NULL CHECK (status IN ('RUNNING', 'UNDER_MAINTENANCE', 'FAILED')),
    hourly_production_value NUMERIC(19,4) NOT NULL,
    replacement_cost NUMERIC(19,4) NOT NULL,
    criticality_level VARCHAR(20) NOT NULL CHECK (criticality_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    age INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS maintenance_parts (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    reference_code VARCHAR(120) NOT NULL UNIQUE,
    unit_cost NUMERIC(19,4) NOT NULL,
    stock_quantity INTEGER NOT NULL,
    lead_time_days INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS maintenance_budgets (
    id BIGSERIAL PRIMARY KEY,
    department VARCHAR(100) NOT NULL,
    period VARCHAR(30) NOT NULL,
    allocated_amount NUMERIC(19,4) NOT NULL,
    spent_amount NUMERIC(19,4) NOT NULL,
    remaining_amount NUMERIC(19,4) NOT NULL
);

CREATE TABLE IF NOT EXISTS maintenance_actions (
    id BIGSERIAL PRIMARY KEY,
    machine_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('PREVENTIVE', 'CORRECTIVE')),
    estimated_duration_hours DOUBLE PRECISION NOT NULL,
    labor_cost_per_hour NUMERIC(19,4) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PLANNED', 'IN_PROGRESS', 'COMPLETED')),
    scheduled_date TIMESTAMP NOT NULL,
    CONSTRAINT fk_maintenance_action_machine FOREIGN KEY (machine_id)
        REFERENCES maintenance_cost_machines(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS maintenance_action_parts (
    maintenance_action_id BIGINT NOT NULL,
    maintenance_part_id BIGINT NOT NULL,
    PRIMARY KEY (maintenance_action_id, maintenance_part_id),
    CONSTRAINT fk_maintenance_action_parts_action FOREIGN KEY (maintenance_action_id)
        REFERENCES maintenance_actions(id) ON DELETE CASCADE,
    CONSTRAINT fk_maintenance_action_parts_part FOREIGN KEY (maintenance_part_id)
        REFERENCES maintenance_parts(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS failure_events (
    id BIGSERIAL PRIMARY KEY,
    machine_id BIGINT NOT NULL,
    failure_type VARCHAR(255) NOT NULL,
    actual_downtime_hours DOUBLE PRECISION NOT NULL,
    total_cost_incurred NUMERIC(19,4) NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_failure_event_machine FOREIGN KEY (machine_id)
        REFERENCES maintenance_cost_machines(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_maintenance_actions_machine_id ON maintenance_actions(machine_id);
CREATE INDEX IF NOT EXISTS idx_failure_events_machine_id ON failure_events(machine_id);