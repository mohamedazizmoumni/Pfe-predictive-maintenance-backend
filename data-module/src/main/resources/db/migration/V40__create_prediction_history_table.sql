-- Create prediction_history table for storing ML predictions and telemetry snapshots
-- Using IF NOT EXISTS to handle cases where table was created by previous migration attempts

CREATE TABLE IF NOT EXISTS prediction_history (
    id BIGSERIAL PRIMARY KEY,
    machine_id BIGINT NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    
    -- Telemetry Snapshot
    health DOUBLE PRECISION,
    risk_score DOUBLE PRECISION,
    remaining_useful_life DOUBLE PRECISION,
    temperature DOUBLE PRECISION,
    vibration DOUBLE PRECISION,
    power_consumption DOUBLE PRECISION,
    pressure DOUBLE PRECISION,
    current DOUBLE PRECISION,
    voltage DOUBLE PRECISION,
    bearing_wear DOUBLE PRECISION,
    thermal_stress DOUBLE PRECISION,
    lubrication_level DOUBLE PRECISION,
    fatigue_index DOUBLE PRECISION,
    efficiency_score DOUBLE PRECISION,
    load_factor DOUBLE PRECISION,
    operating_speed DOUBLE PRECISION,
    ambient_temperature DOUBLE PRECISION,
    
    -- ML Prediction Results
    predicted_rul DOUBLE PRECISION,
    anomaly_probability DOUBLE PRECISION,
    risk_level VARCHAR(20),
    failure_probability DOUBLE PRECISION,
    predicted_failure_type VARCHAR(50),
    recommended_action VARCHAR(500),
    confidence_score DOUBLE PRECISION,
    anomaly_type VARCHAR(50),
    severity DOUBLE PRECISION,
    requires_immediate_action BOOLEAN DEFAULT FALSE,
    
    -- Metadata
    ml_prediction_available BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Add foreign key constraint if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'fk_prediction_machine'
    ) THEN
        ALTER TABLE prediction_history 
        ADD CONSTRAINT fk_prediction_machine 
        FOREIGN KEY (machine_id) REFERENCES machines(id) ON DELETE CASCADE;
    END IF;
END $$;

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_prediction_machine_id ON prediction_history(machine_id);
CREATE INDEX IF NOT EXISTS idx_prediction_timestamp ON prediction_history(timestamp);
CREATE INDEX IF NOT EXISTS idx_prediction_risk_level ON prediction_history(risk_level);
CREATE INDEX IF NOT EXISTS idx_prediction_requires_action ON prediction_history(requires_immediate_action);
CREATE INDEX IF NOT EXISTS idx_prediction_machine_timestamp ON prediction_history(machine_id, timestamp DESC);

-- Add comment
COMMENT ON TABLE prediction_history IS 'Stores historical ML predictions and telemetry snapshots for trend analysis and model retraining';

