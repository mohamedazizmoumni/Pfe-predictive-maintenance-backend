-- V45: Create machine_technician_assignments table
-- Purpose: Track which technicians are assigned to which machines, and by whom

CREATE TABLE IF NOT EXISTS machine_technician_assignments (
    id BIGSERIAL PRIMARY KEY,
    machine_id BIGINT NOT NULL,
    technician_id BIGINT NOT NULL,
    assigned_by_id BIGINT NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_mta_machine FOREIGN KEY (machine_id) REFERENCES machines(id) ON DELETE CASCADE,
    CONSTRAINT fk_mta_technician FOREIGN KEY (technician_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_mta_assigned_by FOREIGN KEY (assigned_by_id) REFERENCES users(id),
    CONSTRAINT uq_mta_machine_technician UNIQUE (machine_id, technician_id)
);

CREATE INDEX IF NOT EXISTS idx_mta_machine_id ON machine_technician_assignments(machine_id);
CREATE INDEX IF NOT EXISTS idx_mta_technician_id ON machine_technician_assignments(technician_id);

COMMENT ON TABLE machine_technician_assignments IS 'Tracks which technicians are assigned to which machines, and by whom';
