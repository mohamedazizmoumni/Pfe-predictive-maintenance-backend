-- Create machine failure report tables required by machine-module entities

CREATE TABLE IF NOT EXISTS machine_failure_reports (
    id BIGSERIAL PRIMARY KEY,
    machine_id BIGINT NOT NULL,
    machine_name VARCHAR(255) NOT NULL,
    current_sensor_state VARCHAR(4000) NOT NULL,
    predicted_failure_days INTEGER NOT NULL,
    risk DOUBLE PRECISION NOT NULL,
    recommended_action VARCHAR(1000) NOT NULL,
    estimated_cost NUMERIC(19,4) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS machine_failure_report_parts (
    id BIGSERIAL PRIMARY KEY,
    report_id BIGINT NOT NULL,
    part_id BIGINT NOT NULL,
    part_name VARCHAR(255) NOT NULL,
    quantity_needed INTEGER NOT NULL,
    current_stock INTEGER NOT NULL,
    minimum_stock INTEGER NOT NULL,
    CONSTRAINT fk_machine_failure_report_parts_report FOREIGN KEY (report_id) REFERENCES machine_failure_reports(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_machine_failure_reports_machine_id ON machine_failure_reports(machine_id);
CREATE INDEX IF NOT EXISTS idx_machine_failure_report_parts_report_id ON machine_failure_report_parts(report_id);
