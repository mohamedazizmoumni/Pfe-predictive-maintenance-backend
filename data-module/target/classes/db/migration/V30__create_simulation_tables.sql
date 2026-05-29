-- ============================================================================
-- MIGRATION V30: Create Simulation Tables
-- Description: Physics-based machine degradation simulation system
-- Author: System
-- Date: 2026-05-07
-- ============================================================================

-- ============================================================================
-- TABLE: machine_simulation_state
-- Purpose: Store persistent degradation state for each machine (SINGLE SOURCE OF TRUTH)
-- ============================================================================
CREATE TABLE machine_simulation_state (
    id BIGSERIAL PRIMARY KEY,
    machine_id BIGINT NOT NULL UNIQUE,
    
    -- Core degradation metrics (0-100)
    health DOUBLE PRECISION NOT NULL DEFAULT 100.0,
    bearing_wear DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    thermal_stress DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    lubrication_level DOUBLE PRECISION NOT NULL DEFAULT 100.0,
    fatigue_index DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    efficiency_score DOUBLE PRECISION NOT NULL DEFAULT 100.0,
    
    -- Operational metrics
    operating_hours BIGINT NOT NULL DEFAULT 0,
    cycles_since_last_maintenance INT NOT NULL DEFAULT 0,
    last_maintenance_date TIMESTAMP,
    
    -- Environmental factors
    ambient_temperature DOUBLE PRECISION NOT NULL DEFAULT 70.0,
    load_factor DOUBLE PRECISION NOT NULL DEFAULT 0.5,
    operating_speed DOUBLE PRECISION NOT NULL DEFAULT 50.0,
    
    -- Metadata
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_machine_state_machine 
        FOREIGN KEY (machine_id) REFERENCES machines(id) ON DELETE CASCADE
);

-- Index for machine_id lookups
CREATE INDEX idx_machine_state_machine_id ON machine_simulation_state(machine_id);

-- Index for health-based queries
CREATE INDEX idx_machine_state_health ON machine_simulation_state(health);

-- Index for critical machines
CREATE INDEX idx_machine_state_bearing_wear ON machine_simulation_state(bearing_wear);

-- ============================================================================
-- TABLE: sensor_telemetry
-- Purpose: Time-series storage for all sensor readings (NO OVERWRITE)
-- ============================================================================
CREATE TABLE sensor_telemetry (
    id BIGSERIAL PRIMARY KEY,
    machine_id BIGINT NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    
    -- Operational settings (3)
    setting1 DOUBLE PRECISION,  -- Ambient temperature
    setting2 DOUBLE PRECISION,  -- Load factor
    setting3 DOUBLE PRECISION,  -- Operating speed (normalized)
    
    -- Sensor readings (21)
    sensor1 DOUBLE PRECISION,   -- Temperature (°F)
    sensor2 DOUBLE PRECISION,   -- Vibration (mm/s)
    sensor3 DOUBLE PRECISION,   -- Power consumption (kW)
    sensor4 DOUBLE PRECISION,   -- Pressure (PSI)
    sensor5 DOUBLE PRECISION,   -- Acoustic emission (dB)
    sensor6 DOUBLE PRECISION,   -- Current draw (A)
    sensor7 DOUBLE PRECISION,   -- Voltage (V)
    sensor8 DOUBLE PRECISION,   -- Torque (Nm)
    sensor9 DOUBLE PRECISION,   -- Flow rate (L/min)
    sensor10 DOUBLE PRECISION,  -- Oil temperature (°F)
    sensor11 DOUBLE PRECISION,  -- Oil pressure (PSI)
    sensor12 DOUBLE PRECISION,  -- Coolant temperature (°F)
    sensor13 DOUBLE PRECISION,  -- Coolant flow (L/min)
    sensor14 DOUBLE PRECISION,  -- Bearing temperature (°F)
    sensor15 DOUBLE PRECISION,  -- Motor temperature (°F)
    sensor16 DOUBLE PRECISION,  -- Humidity (%)
    sensor17 DOUBLE PRECISION,  -- Noise level (dB)
    sensor18 DOUBLE PRECISION,  -- Rotation speed (RPM)
    sensor19 DOUBLE PRECISION,  -- Efficiency (%)
    sensor20 DOUBLE PRECISION,  -- Thrust (N)
    sensor21 DOUBLE PRECISION,  -- Fuel flow (L/h)
    
    -- Metadata
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_sensor_telemetry_machine 
        FOREIGN KEY (machine_id) REFERENCES machines(id) ON DELETE CASCADE
);

-- Critical indexes for time-series queries
CREATE INDEX idx_sensor_telemetry_machine_time 
    ON sensor_telemetry(machine_id, timestamp DESC);

CREATE INDEX idx_sensor_telemetry_timestamp 
    ON sensor_telemetry(timestamp DESC);

-- Note: Partial index with NOW() removed due to PostgreSQL immutability requirement
-- Applications should handle 24h filtering in queries instead

-- ============================================================================
-- TABLE: machine_events
-- Purpose: Store detected events (spikes, anomalies, threshold crossings)
-- ============================================================================
CREATE TABLE machine_events (
    id BIGSERIAL PRIMARY KEY,
    machine_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    event_timestamp TIMESTAMP NOT NULL,
    severity VARCHAR(20) NOT NULL,
    sensor_name VARCHAR(50),
    sensor_value DOUBLE PRECISION,
    threshold_value DOUBLE PRECISION,
    description TEXT,
    action_taken VARCHAR(255),
    acknowledged BOOLEAN DEFAULT FALSE,
    acknowledged_by VARCHAR(255),
    acknowledged_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_machine_events_machine 
        FOREIGN KEY (machine_id) REFERENCES machines(id) ON DELETE CASCADE
);

-- Indexes for event queries
CREATE INDEX idx_machine_events_machine_time 
    ON machine_events(machine_id, event_timestamp DESC);

CREATE INDEX idx_machine_events_type 
    ON machine_events(event_type);

CREATE INDEX idx_machine_events_severity 
    ON machine_events(severity);

CREATE INDEX idx_machine_events_unacknowledged 
    ON machine_events(machine_id, acknowledged) 
    WHERE acknowledged = FALSE;

-- ============================================================================
-- COMMENTS
-- ============================================================================
COMMENT ON TABLE machine_simulation_state IS 'Physics-based degradation state - single source of truth for machine condition';
COMMENT ON TABLE sensor_telemetry IS 'Time-series sensor data derived from degradation state';
COMMENT ON TABLE machine_events IS 'Real-time event detection (spikes, anomalies, threshold crossings)';

COMMENT ON COLUMN machine_simulation_state.bearing_wear IS 'Bearing wear level (0-100) - increases with friction';
COMMENT ON COLUMN machine_simulation_state.thermal_stress IS 'Thermal stress (0-100) - increases with thermal cycling';
COMMENT ON COLUMN machine_simulation_state.lubrication_level IS 'Lubrication quality (0-100) - depletes with usage';
COMMENT ON COLUMN machine_simulation_state.fatigue_index IS 'Fatigue index (0-100) - accumulates with cyclic loading';
COMMENT ON COLUMN machine_simulation_state.efficiency_score IS 'Efficiency score (0-100) - decreases with degradation';
COMMENT ON COLUMN machine_simulation_state.health IS 'Overall health (0-100) - weighted average of all metrics';

-- ============================================================================
-- INITIAL DATA (Optional)
-- ============================================================================
-- Initialize simulation state for existing machines
INSERT INTO machine_simulation_state (
    machine_id, health, bearing_wear, thermal_stress, lubrication_level, 
    fatigue_index, efficiency_score, operating_hours, cycles_since_last_maintenance,
    last_maintenance_date, ambient_temperature, load_factor, operating_speed,
    created_at, updated_at
)
SELECT 
    id,
    100.0,  -- health
    0.0,    -- bearing_wear
    0.0,    -- thermal_stress
    100.0,  -- lubrication_level
    0.0,    -- fatigue_index
    100.0,  -- efficiency_score
    COALESCE(operating_hours, 0),
    0,      -- cycles_since_last_maintenance
    COALESCE(last_maintenance_date, NOW()),
    70.0,   -- ambient_temperature
    0.5,    -- load_factor
    50.0,   -- operating_speed
    NOW(),
    NOW()
FROM machines
WHERE NOT EXISTS (
    SELECT 1 FROM machine_simulation_state WHERE machine_id = machines.id
);

-- ============================================================================
-- GRANTS (if needed)
-- ============================================================================
-- GRANT SELECT, INSERT, UPDATE, DELETE ON machine_simulation_state TO app_user;
-- GRANT SELECT, INSERT ON sensor_telemetry TO app_user;
-- GRANT SELECT, INSERT, UPDATE ON machine_events TO app_user;
-- GRANT USAGE, SELECT ON SEQUENCE sensor_telemetry_id_seq TO app_user;
-- GRANT USAGE, SELECT ON SEQUENCE machine_events_id_seq TO app_user;
