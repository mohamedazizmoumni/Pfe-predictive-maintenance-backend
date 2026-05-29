-- Align sensor_data schema with SensorData entity
-- Existing table uses legacy "timestamp" column; entity expects "recorded_date".

ALTER TABLE sensor_data
    ADD COLUMN IF NOT EXISTS recorded_date TIMESTAMP;

UPDATE sensor_data
SET recorded_date = COALESCE(recorded_date, timestamp, created_date, CURRENT_TIMESTAMP)
WHERE recorded_date IS NULL;

ALTER TABLE sensor_data
    ALTER COLUMN recorded_date SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_sensor_data_recorded_date ON sensor_data(recorded_date DESC);
CREATE INDEX IF NOT EXISTS idx_sensor_data_sensor_recorded_date ON sensor_data(sensor_id, recorded_date DESC);

COMMENT ON COLUMN sensor_data.recorded_date IS 'Timestamp when the sensor reading was recorded';
