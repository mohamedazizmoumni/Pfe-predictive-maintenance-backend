-- Customer Portal foundation: a CUSTOMER role, row-level machine scoping via
-- customer_machine_links, and support_tickets. This is the platform's first
-- external-facing role, reusing the existing users/roles/JWT infrastructure
-- (see V2/V16/V36 for the same role-add pattern) rather than a parallel
-- auth system.

INSERT INTO roles (name, description) VALUES
('CUSTOMER', 'External equipment owner with read-only access to their own linked machines via the customer portal')
ON CONFLICT (name) DO NOTHING;

INSERT INTO permissions (name, description) VALUES
('VIEW_OWN_MACHINES', 'Can view machines linked to their customer account'),
('CREATE_SUPPORT_TICKET', 'Can create support tickets for their own machines')
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'CUSTOMER' AND p.name IN ('VIEW_OWN_MACHINES', 'CREATE_SUPPORT_TICKET')
ON CONFLICT DO NOTHING;

CREATE TABLE IF NOT EXISTS customer_machine_links (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    machine_id BIGINT       NOT NULL REFERENCES machines (id) ON DELETE CASCADE,
    linked_by  VARCHAR(100),
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_customer_machine_link UNIQUE (user_id, machine_id)
);

CREATE INDEX IF NOT EXISTS idx_customer_machine_links_user ON customer_machine_links (user_id);
CREATE INDEX IF NOT EXISTS idx_customer_machine_links_machine ON customer_machine_links (machine_id);

CREATE TABLE IF NOT EXISTS support_tickets (
    id                BIGSERIAL PRIMARY KEY,
    customer_user_id  BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    machine_id        BIGINT        NOT NULL REFERENCES machines (id) ON DELETE CASCADE,
    subject           VARCHAR(255)  NOT NULL,
    description       TEXT,
    status            VARCHAR(20)   NOT NULL DEFAULT 'OPEN',
    assigned_to       VARCHAR(100),
    resolution_notes  TEXT,
    resolved_at       TIMESTAMP,
    created_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_support_tickets_customer ON support_tickets (customer_user_id);
CREATE INDEX IF NOT EXISTS idx_support_tickets_machine ON support_tickets (machine_id);
CREATE INDEX IF NOT EXISTS idx_support_tickets_status ON support_tickets (status);

COMMENT ON TABLE customer_machine_links IS 'Row-level scope: which machines a CUSTOMER user may see via the portal';
COMMENT ON TABLE support_tickets IS 'Customer-raised support requests, tied to one of their linked machines';

-- Demo customer account, same seeded-password convention as V36's
-- financemanager/stockmanager test accounts (bcrypt hash shared across all
-- non-superadmin demo accounts in this repo).
INSERT INTO users (username, email, password, first_name, last_name, display_name, department, status, locked, mfa_enabled, created_date)
VALUES ('customer', 'customer@predictive-maintenance.local',
        '$2a$10$slYQmyNdGzin7olVeY5/OPST9/PgBkqquzi8Ay0IQI7tY4xZS2Dmy',
        'Demo', 'Customer', 'Demo Customer', 'External', 'ACTIVE', false, false, NOW())
ON CONFLICT (username) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'customer' AND r.name = 'CUSTOMER'
ON CONFLICT DO NOTHING;

-- Link the demo customer to the first machine that exists at migration time,
-- if any (demo data is seeded by the Java initializers, which may or may not
-- have run yet depending on startup order — this is a best-effort convenience
-- link, not a guarantee; use POST /api/v1/portal-admin/links for real linking).
INSERT INTO customer_machine_links (user_id, machine_id, linked_by)
SELECT u.id, m.id, 'SYSTEM_SEED'
FROM users u, machines m
WHERE u.username = 'customer'
ORDER BY m.id
LIMIT 1
ON CONFLICT DO NOTHING;
