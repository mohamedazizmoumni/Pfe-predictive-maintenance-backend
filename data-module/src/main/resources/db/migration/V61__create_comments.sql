-- Generic threaded comments on Machines and Maintenance work orders.
CREATE TABLE IF NOT EXISTS comments (
    id               BIGSERIAL PRIMARY KEY,
    entity_type      VARCHAR(50)   NOT NULL,
    entity_id        BIGINT        NOT NULL,
    author_username  VARCHAR(100)  NOT NULL,
    body             VARCHAR(2000) NOT NULL,
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_comments_entity ON comments (entity_type, entity_id);
