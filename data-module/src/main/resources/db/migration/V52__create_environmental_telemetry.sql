-- Real environmental sensor readings (e.g. ESP32 + DHT11 temperature/humidity)
-- attached to an existing machine as a second, independent telemetry stream.
-- Not consumed by the C-MAPSS-trained RUL model (sensor_telemetry) - scored by
-- a separate environment-risk analysis path instead.
CREATE TABLE IF NOT EXISTS environmental_telemetry (
    id           BIGSERIAL PRIMARY KEY,
    machine_id   BIGINT           NOT NULL REFERENCES machines (id) ON DELETE CASCADE,
    temperature  DOUBLE PRECISION NOT NULL,
    humidity     DOUBLE PRECISION NOT NULL,
    risk_level   VARCHAR(20),
    timestamp    TIMESTAMP        NOT NULL,
    created_at   TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_env_machine_timestamp
    ON environmental_telemetry (machine_id, timestamp DESC);

COMMENT ON TABLE environmental_telemetry IS 'Real (non-simulated) sensor readings from external IoT hardware, e.g. ESP32+DHT11, linked to an existing machine';
