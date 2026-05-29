-- Align sensor_data.value with SensorData entity (Double -> DOUBLE PRECISION)

ALTER TABLE sensor_data
    ALTER COLUMN value TYPE DOUBLE PRECISION USING value::DOUBLE PRECISION;

COMMENT ON COLUMN sensor_data.value IS 'Sensor reading value stored as double precision for Hibernate validation';
