-- Generic per-user key/value preferences store (notification prefs,
-- dashboard widget layout, etc.) — one row per (user_id, pref_key).
CREATE TABLE IF NOT EXISTS user_preferences (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    pref_key            VARCHAR(100)  NOT NULL,
    pref_value          VARCHAR(4000) NOT NULL,
    created_date        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date  TIMESTAMP,
    CONSTRAINT uq_user_preferences_user_key UNIQUE (user_id, pref_key)
);
