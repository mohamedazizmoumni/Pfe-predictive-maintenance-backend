-- Add optimistic lock column required by SensorData entity

ALTER TABLE sensor_data
    ADD COLUMN IF NOT EXISTS version BIGINT;

UPDATE sensor_data
SET version = COALESCE(version, 0)
WHERE version IS NULL;

ALTER TABLE sensor_data
    ALTER COLUMN version SET DEFAULT 0;
