-- Machine Module Migration
-- V4: Create machine, sensor, and sensor data tables
-- Purpose: Foundation for predictive maintenance machine monitoring

-- ==================== MACHINES TABLE ====================
CREATE TABLE IF NOT EXISTS machines (
    id BIGSERIAL PRIMARY KEY,
    serial_number VARCHAR(100) NOT NULL UNIQUE,
    model VARCHAR(255) NOT NULL,
    location VARCHAR(255) NOT NULL,
    manufacturer VARCHAR(255),
    installation_year INTEGER,
    installed_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'OPERATIONAL' CHECK (status IN ('OPERATIONAL', 'MAINTENANCE', 'FAULTY', 'INACTIVE')),
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    last_modified_by VARCHAR(100),
    version INTEGER DEFAULT 0,
    
    CONSTRAINT chk_installation_year CHECK (installation_year >= 1900 AND installation_year <= 2100)
);

CREATE INDEX idx_machines_serial_number ON machines(serial_number);
CREATE INDEX idx_machines_status ON machines(status);
CREATE INDEX idx_machines_location ON machines(location);
CREATE INDEX idx_machines_model ON machines(model);
CREATE INDEX idx_machines_created_date ON machines(created_date);

-- ==================== SENSORS TABLE ====================
CREATE TABLE IF NOT EXISTS sensors (
    id BIGSERIAL PRIMARY KEY,
    machine_id BIGINT NOT NULL,
    sensor_type VARCHAR(100) NOT NULL,
    unit VARCHAR(50),
    min_threshold NUMERIC(10,2),
    max_threshold NUMERIC(10,2),
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    last_modified_by VARCHAR(100),
    version INTEGER DEFAULT 0,
    
    CONSTRAINT fk_sensors_machine FOREIGN KEY (machine_id) REFERENCES machines(id) ON DELETE CASCADE,
    CONSTRAINT uk_machine_sensor_type UNIQUE (machine_id, sensor_type),
    CONSTRAINT chk_thresholds CHECK (min_threshold <= max_threshold OR min_threshold IS NULL OR max_threshold IS NULL)
);

CREATE INDEX idx_sensors_machine_id ON sensors(machine_id);
CREATE INDEX idx_sensors_sensor_type ON sensors(sensor_type);
CREATE INDEX idx_sensors_created_date ON sensors(created_date);

-- ==================== SENSOR DATA TABLE ====================
CREATE TABLE IF NOT EXISTS sensor_data (
    id BIGSERIAL PRIMARY KEY,
    sensor_id BIGINT NOT NULL,
    value NUMERIC(12,4) NOT NULL,
    unit VARCHAR(50),
    is_anomaly BOOLEAN DEFAULT FALSE,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes TEXT,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    
    CONSTRAINT fk_sensor_data_sensor FOREIGN KEY (sensor_id) REFERENCES sensors(id) ON DELETE CASCADE,
    CONSTRAINT chk_value_not_negative CHECK (value >= 0)
);

CREATE INDEX idx_sensor_data_sensor_id ON sensor_data(sensor_id);
CREATE INDEX idx_sensor_data_timestamp ON sensor_data(timestamp DESC);
CREATE INDEX idx_sensor_data_is_anomaly ON sensor_data(is_anomaly);
CREATE INDEX idx_sensor_data_sensor_timestamp ON sensor_data(sensor_id, timestamp DESC);
CREATE INDEX idx_sensor_data_anomaly_time ON sensor_data(sensor_id, is_anomaly, timestamp DESC);

-- ==================== COMMENT ====================
COMMENT ON TABLE machines IS 'Stores machine information for predictive maintenance monitoring';
COMMENT ON TABLE sensors IS 'Stores sensor information attached to machines';
COMMENT ON TABLE sensor_data IS 'Stores timestamped sensor readings and anomaly flags';

COMMENT ON COLUMN machines.status IS 'Machine operational status: OPERATIONAL, MAINTENANCE, FAULTY, INACTIVE';
COMMENT ON COLUMN sensors.min_threshold IS 'Minimum acceptable sensor value; readings below trigger anomaly';
COMMENT ON COLUMN sensors.max_threshold IS 'Maximum acceptable sensor value; readings above trigger anomaly';
COMMENT ON COLUMN sensor_data.is_anomaly IS 'TRUE if value is outside sensor thresholds, indicating potential issue';
