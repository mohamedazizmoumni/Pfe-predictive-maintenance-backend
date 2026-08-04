-- Completes the Customer Portal: warranties, invoices, and ticket messaging
-- (2026-08-01/02 scope reorientation — Customer Portal is the one net-new
-- functional area, everything else extends the existing 7 roles).

CREATE TABLE IF NOT EXISTS warranties (
    id           BIGSERIAL PRIMARY KEY,
    machine_id   BIGINT        NOT NULL REFERENCES machines (id) ON DELETE CASCADE,
    provider     VARCHAR(255)  NOT NULL,
    start_date   DATE          NOT NULL,
    end_date     DATE          NOT NULL,
    terms        VARCHAR(2000),
    created_date TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_warranties_machine_id ON warranties (machine_id);

CREATE TABLE IF NOT EXISTS invoices (
    id                BIGSERIAL PRIMARY KEY,
    customer_user_id  BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    machine_id        BIGINT        REFERENCES machines (id) ON DELETE SET NULL,
    invoice_number    VARCHAR(100)  NOT NULL,
    amount            NUMERIC(19,2) NOT NULL,
    status            VARCHAR(20)   NOT NULL DEFAULT 'UNPAID',
    issue_date        DATE          NOT NULL,
    due_date          DATE          NOT NULL,
    description       VARCHAR(1000),
    created_date      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_invoices_customer ON invoices (customer_user_id);

CREATE TABLE IF NOT EXISTS portal_messages (
    id               BIGSERIAL PRIMARY KEY,
    ticket_id        BIGINT        NOT NULL REFERENCES support_tickets (id) ON DELETE CASCADE,
    sender_username  VARCHAR(100)  NOT NULL,
    from_customer    BOOLEAN       NOT NULL,
    body             VARCHAR(2000) NOT NULL,
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_portal_messages_ticket ON portal_messages (ticket_id);

COMMENT ON TABLE warranties IS 'Machine warranty coverage, visible to the linked customer via the portal';
COMMENT ON TABLE invoices IS 'Customer invoices, visible to the owning customer via the portal';
COMMENT ON TABLE portal_messages IS 'Message thread on a support ticket between a customer and Manager/Admin';
