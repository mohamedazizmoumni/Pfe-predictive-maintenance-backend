-- Create prediction_records table required by PredictionRecord entity

CREATE TABLE IF NOT EXISTS prediction_records (
    id BIGSERIAL PRIMARY KEY,
    machine_id BIGINT NOT NULL,
    predicted_at TIMESTAMP NOT NULL,
    rul_value DOUBLE PRECISION NOT NULL,
    confidence_low DOUBLE PRECISION,
    confidence_high DOUBLE PRECISION,
    risk_level VARCHAR(20) NOT NULL,
    model_version VARCHAR(50),
    triggered_by VARCHAR(100),
    input_features_summary TEXT,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT,
    CONSTRAINT fk_prediction_records_machine FOREIGN KEY (machine_id) REFERENCES machines(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_prediction_machine_id ON prediction_records(machine_id);
CREATE INDEX IF NOT EXISTS idx_prediction_timestamp ON prediction_records(predicted_at DESC);
CREATE INDEX IF NOT EXISTS idx_prediction_machine_time ON prediction_records(machine_id, predicted_at DESC);

-- Align notification FK with PredictionRecord entity table
DO $$
BEGIN
    IF to_regclass('public.notifications') IS NOT NULL THEN
        IF EXISTS (
            SELECT 1
            FROM information_schema.table_constraints
            WHERE table_name = 'notifications'
              AND constraint_name = 'fk_notifications_prediction'
        ) THEN
            ALTER TABLE notifications DROP CONSTRAINT fk_notifications_prediction;
        END IF;

        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.table_constraints
            WHERE table_name = 'notifications'
              AND constraint_name = 'fk_notifications_prediction'
        ) THEN
            ALTER TABLE notifications
                ADD CONSTRAINT fk_notifications_prediction
                FOREIGN KEY (prediction_record_id) REFERENCES prediction_records(id) ON DELETE SET NULL;
        END IF;
    END IF;
END
$$;
