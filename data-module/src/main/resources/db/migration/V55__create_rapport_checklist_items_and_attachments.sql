-- Structured inspection checklist per rapport, replacing the free-text-only
-- workPerformed field for pass/fail line items (functional audit + enterprise
-- blueprint §09 Checklist/ChecklistItem, §04 rule #29).
CREATE TABLE IF NOT EXISTS rapport_checklist_items (
    rapport_id  BIGINT       NOT NULL REFERENCES maintenance_rapports (id) ON DELETE CASCADE,
    description VARCHAR(500) NOT NULL,
    passed      BOOLEAN      NOT NULL DEFAULT TRUE,
    notes       VARCHAR(1000)
);

CREATE INDEX IF NOT EXISTS idx_rapport_checklist_items_rapport_id ON rapport_checklist_items (rapport_id);

COMMENT ON TABLE rapport_checklist_items IS 'Technician inspection checklist lines on a maintenance rapport';

-- Generic, polymorphic evidence attachment (photos, eventually video/docs) —
-- entity_type/entity_id are plain descriptive columns, not a relation, so this
-- table can attach to any record (rapports today, machines/tickets later)
-- without a schema change (blueprint §09 Attachment).
CREATE TABLE IF NOT EXISTS attachments (
    id                BIGSERIAL PRIMARY KEY,
    entity_type       VARCHAR(100)  NOT NULL,
    entity_id         BIGINT        NOT NULL,
    file_name         VARCHAR(255)  NOT NULL,
    stored_file_name  VARCHAR(255)  NOT NULL,
    content_type      VARCHAR(100),
    file_size         BIGINT,
    uploaded_by       VARCHAR(100)  NOT NULL,
    uploaded_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_attachments_entity ON attachments (entity_type, entity_id);

COMMENT ON TABLE attachments IS 'Polymorphic file attachments (repair evidence photos, etc.) — entity_type/entity_id describe the owning record without a FK';
